package cn.edu.thssdb.index;

import cn.edu.thssdb.utils.Pair;

import java.io.Serializable;
import java.util.UUID;

import static cn.edu.thssdb.utils.Global.INTERNAL;

public final class BPlusTree<K extends Comparable<K>, V>
    implements Iterable<Pair<K, V>>, Serializable {

  public BPlusTreeNode<K, V> root; // recovered by Database.class
  private int size; // recovered by Database.class
  public TreeNodeManager<K, V> nodeManager; // recovered by Database.class

  // only called when create table
  public BPlusTree() {
    root = new BPlusTreeLeafNode<>(0, null);
    root.id = UUID.randomUUID();
    nodeManager = new TreeNodeManager<>(root);
  }

  public int size() {
    return size;
  }

  public V get(K key) {
    if (key == null) throw new IllegalArgumentException("argument key to get() is null");
    return root.get(key, this.nodeManager);
  }

  public void update(K key, V value) {
    root.remove(key, this.nodeManager);
    root.put(key, value, this.nodeManager);
  }

  public void put(K key, V value) {
    if (key == null) throw new IllegalArgumentException("argument key to put() is null");
    root.put(key, value, this.nodeManager);
    size++;
    checkRoot();
  }

  public void remove(K key) {
    if (key == null) throw new IllegalArgumentException("argument key to remove() is null");
    root.remove(key, this.nodeManager);
    size--; // tree size
    if (root instanceof BPlusTreeInternalNode && root.size() == 0) { // if it has leaf
      // root = ((BPlusTreeInternalNode<K, V>) root).children.get(0);

      // modify start
      UUID original_root_id = root.id;
      root = this.nodeManager.loadNode(((BPlusTreeInternalNode<K, V>) root).children_id.get(0));
      // first and the only child
      this.nodeManager.root_id = root.id; // this will be serialized by Table.class
      // load first children and make it root
      root.parent_id = null; // root has no parent node

      this.nodeManager.writeNodeToDisk(root); // persist new root
      this.nodeManager.deleteNode(original_root_id); // delete original root node and files
      // modify end
    }
  }

  public boolean contains(K key) {
    if (key == null) throw new IllegalArgumentException("argument key to contains() is null");
    return root.containsKey(key, this.nodeManager);
  }

  private void checkRoot() { // used when put
    if (root.isOverFlow()) {
      // should load node?
      BPlusTreeNode<K, V> newSiblingNode = root.split(this.nodeManager, null);
      // cache load should be done in split.()

      //BPlusTreeInternalNode<K, V> newRoot = new BPlusTreeInternalNode<>(1);
      BPlusTreeInternalNode<K, V> newRoot =
              (BPlusTreeInternalNode) this.nodeManager.
                      newNode(1, INTERNAL, newSiblingNode.getFirstLeafKey(), null);
      //newRoot.keys.set(0, newSiblingNode.getFirstLeafKey()); -> done in newNode method

      //newRoot.children.set(0, root);
      newRoot.children.set(0, root.keys.get(0));
      newRoot.children_id.set(0, root.id);
      root.parent_id = newRoot.id;
      //newRoot.children.set(1, newSiblingNode);
      newRoot.children.set(1, newSiblingNode.keys.get(0));
      newRoot.children_id.set(1, newSiblingNode.id);
      newSiblingNode.parent_id = newRoot.id;

      this.nodeManager.writeNodeToDisk(root);
      this.nodeManager.writeNodeToDisk(newRoot);
      this.nodeManager.writeNodeToDisk(newSiblingNode);
      // persist modified nodes

      root = newRoot;
      this.nodeManager.root_id = root.id;
    }
  }

  public void clear() {
    root.clear();
  }

  @Override
  public BPlusTreeIterator<K, V> iterator() {
    return new BPlusTreeIterator<>(this);
  }
}
