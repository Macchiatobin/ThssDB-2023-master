package cn.edu.thssdb.index;

import cn.edu.thssdb.exception.DuplicateKeyException;
import cn.edu.thssdb.exception.KeyNotExistException;
import cn.edu.thssdb.utils.Global;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

public class BPlusTreeLeafNode<K extends Comparable<K>, V> extends BPlusTreeNode<K, V> {

  ArrayList<V> values;
  //private BPlusTreeLeafNode<K, V> next; // TODO: need modify, should only save key of next leaf node
  private K next;

  BPlusTreeLeafNode(int size) {
    keys = new ArrayList<>(Collections.nCopies((int) (1.5 * Global.fanout) + 1, null));
    values = new ArrayList<>(Collections.nCopies((int) (1.5 * Global.fanout) + 1, null));
    nodeSize = size;
  }

  private void valuesAdd(int index, V value) {
    for (int i = nodeSize; i > index; i--) values.set(i, values.get(i - 1));
    values.set(index, value);
  }

  private void valuesRemove(int index) {
    for (int i = index; i < nodeSize - 1; i++) values.set(i, values.get(i + 1));
  }

  @Override
  boolean containsKey(K key, TreeNodeManager<K, V> nodeManager) {
    return binarySearch(key) >= 0;
  }

  @Override
  V get(K key, TreeNodeManager<K, V> nodeManager) {
    int index = binarySearch(key);
    if (index >= 0) return values.get(index);
    throw new KeyNotExistException();
  }

  @Override
  void put(K key, V value, TreeNodeManager<K, V> nodeManager) {
    int index = binarySearch(key);
    int valueIndex = index >= 0 ? index : -index - 1;
    if (index >= 0) throw new DuplicateKeyException();
    else {
      valuesAdd(valueIndex, value);
      keysAdd(valueIndex, key);
    }
  }

  @Override
  void remove(K key, TreeNodeManager<K, V> nodeManager) {
    int index = binarySearch(key);
    if (index >= 0) {
      valuesRemove(index);
      keysRemove(index);
    } else throw new KeyNotExistException();
  }

  @Override
  K getFirstLeafKey() {
    return keys.get(0);
  }

  @Override
  BPlusTreeNode<K, V> split(TreeNodeManager<K, V> nodeManager) {
    int from = (size() + 1) / 2;
    int to = size();
    BPlusTreeLeafNode<K, V> newSiblingNode = new BPlusTreeLeafNode<>(to - from);
    // TODO: write to disk and load to cache
    for (int i = 0; i < to - from; i++) {
      newSiblingNode.keys.set(i, keys.get(i + from));
      newSiblingNode.values.set(i, values.get(i + from));
      keys.set(i + from, null);
      values.set(i + from, null);
    }
    nodeSize = from;
    newSiblingNode.next = next;
    next = newSiblingNode;
    return newSiblingNode;
  }

  @Override
  void merge(BPlusTreeNode<K, V> sibling) {
    int index = size();
    BPlusTreeLeafNode<K, V> node = (BPlusTreeLeafNode<K, V>) sibling;
    int length = node.size();
    for (int i = 0; i < length; i++) {
      keys.set(i + index, node.keys.get(i));
      values.set(i + index, node.values.get(i));
    }
    nodeSize = index + length;
    next = node.next;
  }

  @Override
  public void clear() {
    // TODO
  }
}
