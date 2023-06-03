package cn.edu.thssdb.index;

import cn.edu.thssdb.utils.Global;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import static cn.edu.thssdb.utils.Global.INTERNAL;

public final class BPlusTreeInternalNode<K extends Comparable<K>, V> extends BPlusTreeNode<K, V> {

  // ArrayList<BPlusTreeNode<K, V>> children;
  ArrayList<K> children;
  ArrayList<UUID> children_id;

  BPlusTreeInternalNode(int size, UUID parent_id) {
    keys = new ArrayList<>(Collections.nCopies((int) (1.5 * Global.fanout) + 1, null));
    children = new ArrayList<>((Collections.nCopies((int) (1.5 * Global.fanout) + 2, null)));
    children_id = new ArrayList<>((Collections.nCopies((int) (1.5 * Global.fanout) + 2, null)));
    this.nodeSize = size;
    this.parent_id = parent_id;
  }

  // called when put and no such key exists
  // or when split
  private void childrenAdd(int index, BPlusTreeNode<K, V> node) {
    for (int i = nodeSize + 1; i > index; i--) {
      children.set(i, children.get(i - 1));
      children_id.set(i, children_id.get(i - 1));
    }
    children.set(index, node.keys.get(0));
    children_id.set(index, node.id);
    // write to disk is done by caller
  }

  @Override
  boolean containsKey(K key, TreeNodeManager<K, V> nodeManager) {
    // return searchChild(key).containsKey(key, nodeManager);
    BPlusTreeNode<K, V> child_found = nodeManager.loadNode(searchChild(key));
    return child_found.containsKey(key, nodeManager);
  }

  @Override
  V get(K key, TreeNodeManager<K, V> nodeManager) {
    // return searchChild(key).get(key, nodeManager);
    BPlusTreeNode<K, V> child_found = nodeManager.loadNode(searchChild(key));
    return child_found.get(key, nodeManager);
  }

  @Override
  void put(K key, V value, TreeNodeManager<K, V> nodeManager) {
    // BPlusTreeNode<K, V> child = searchChild(key);
    BPlusTreeNode<K, V> child = nodeManager.loadNode(searchChild(key));

    child.put(key, value, nodeManager);

    if (child.isOverFlow()) {
      BPlusTreeNode<K, V> newSiblingNode = child.split(nodeManager, this.id);
      insertChild(newSiblingNode.getFirstLeafKey(nodeManager), newSiblingNode);

      nodeManager.writeNodeToDisk(child); // repeated if overflow
      nodeManager.writeNodeToDisk(newSiblingNode);
      nodeManager.writeNodeToDisk(this);
    }
  }

  @Override
  void remove(K key, TreeNodeManager<K, V> nodeManager) {
    int index = binarySearch(key);
    int childIndex = index >= 0 ? index + 1 : -index - 1;
    // BPlusTreeNode<K, V> child = children.get(childIndex);
    BPlusTreeNode<K, V> child = nodeManager.loadNode(children_id.get(childIndex));
    child.remove(key, nodeManager);

    if (child.isUnderFlow()) {
      // BPlusTreeNode<K, V> childLeftSibling = getChildLeftSibling(key);
      BPlusTreeNode<K, V> childLeftSibling = nodeManager.loadNode(getChildLeftSiblingId(key));
      // BPlusTreeNode<K, V> childRightSibling = getChildRightSiblingId(key);
      BPlusTreeNode<K, V> childRightSibling = nodeManager.loadNode(getChildRightSiblingId(key));
      BPlusTreeNode<K, V> left = childLeftSibling != null ? childLeftSibling : child;
      BPlusTreeNode<K, V> right = childLeftSibling != null ? child : childRightSibling;
      // If left child exists, left = left child / right = child
      // else left = child / right = right child

      left.merge(right, nodeManager);
      if (index >= 0) { // key to remove in this.keys
        childrenRemove(index + 1); // remove corresponding child
        keysRemove(index); // and key
      } else { // key to remove not in this.keys
        assert right != null;
        deleteChild(right.getFirstLeafKey(nodeManager));
      }

      nodeManager.deleteNode(right.id); // TODO: check if this is right

      if (left.isOverFlow()) {
        BPlusTreeNode<K, V> newSiblingNode = left.split(nodeManager, this.id);
        insertChild(newSiblingNode.getFirstLeafKey(nodeManager), newSiblingNode);

        nodeManager.writeNodeToDisk(newSiblingNode);
      }
      nodeManager.writeNodeToDisk(left);
    } else if (index >= 0) { // in case of changes made in children
      BPlusTreeNode<K, V> child_to_check = nodeManager.loadNode(children_id.get(index + 1));
      keys.set(index, child_to_check.getFirstLeafKey(nodeManager));
    }
    nodeManager.writeNodeToDisk(this);
    // else, write to disk is done by child.remove()
  }

  @Override
  K getFirstLeafKey(TreeNodeManager<K, V> nodeManager) {
    // return children.get(0).getFirstLeafKey();
    BPlusTreeNode<K, V> first_child = nodeManager.loadNode(this.children_id.get(0));
    return first_child.getFirstLeafKey(nodeManager);
  }

  @Override
  BPlusTreeNode<K, V> split(TreeNodeManager<K, V> nodeManager, UUID parent_id) {
    int from = size() / 2 + 1;
    int to = size();
    // BPlusTreeInternalNode<K, V> newSiblingNode = new BPlusTreeInternalNode<>(to - from,
    // parent_id);
    BPlusTreeInternalNode<K, V> newSiblingNode =
        (BPlusTreeInternalNode<K, V>) nodeManager.newNode(to - from, INTERNAL, null, parent_id);
    // load to cache is done by manager
    for (int i = 0; i < to - from; i++) {
      newSiblingNode.keys.set(i, keys.get(i + from));
      newSiblingNode.children.set(i, children.get(i + from));
      newSiblingNode.children_id.set(i, children_id.get(i + from));
    }
    newSiblingNode.children.set(to - from, children.get(to));
    newSiblingNode.children_id.set(to - from, children_id.get(to));
    this.nodeSize = this.nodeSize - to + from - 1;
    return newSiblingNode;
    // write to disk is done by caller
  }

  @Override
  void merge(BPlusTreeNode<K, V> sibling, TreeNodeManager<K, V> nodeManager) {
    int index = nodeSize;
    BPlusTreeInternalNode<K, V> node = (BPlusTreeInternalNode<K, V>) sibling;
    int length = node.nodeSize;
    keys.set(index, node.getFirstLeafKey(nodeManager));
    for (int i = 0; i < length; i++) {
      keys.set(i + index + 1, node.keys.get(i));
      children.set(i + index + 1, node.children.get(i));
      children_id.set(i + index + 1, node.children_id.get(i));
    }
    children.set(length + index + 1, node.children.get(length));
    children_id.set(length + index + 1, node.children_id.get(length));
    nodeSize = index + length + 1;
  }

  @Override
  public void clear() {
    // TODO
  }

  private UUID searchChild(K key) {
    int index = binarySearch(key);
    return children_id.get(index >= 0 ? index + 1 : -index - 1);
  }

  private void insertChild(K key, BPlusTreeNode<K, V> child) {
    int index = binarySearch(key);
    int childIndex = index >= 0 ? index + 1 : -index - 1;
    if (index >= 0) {
      children.set(childIndex, child.keys.get(0));
      children_id.set(childIndex, child.id);
    } else {
      childrenAdd(childIndex + 1, child);
      keysAdd(childIndex, key);
    }
  }

  private void deleteChild(K key) {
    int index = binarySearch(key);
    if (index >= 0) {
      childrenRemove(index + 1);
      keysRemove(index);

      // write to disk done by caller
    }
  }

  private void childrenRemove(int index) {
    for (int i = index; i < nodeSize; i++) {
      children.set(i, children.get(i + 1));
      children_id.set(i, children_id.get(i + 1));
    }
    // write to disk maybe done by caller?
  }

  private UUID getChildLeftSiblingId(K key) {
    int index = binarySearch(key);
    int childIndex = index >= 0 ? index + 1 : -index - 1;
    if (childIndex > 0) return children_id.get(childIndex - 1);
    return null;
  }

  private UUID getChildRightSiblingId(K key) {
    int index = binarySearch(key);
    int childIndex = index >= 0 ? index + 1 : -index - 1;
    if (childIndex < size()) return children_id.get(childIndex + 1);
    return null;
  }
}
