package cn.edu.thssdb.index;

import cn.edu.thssdb.utils.Global;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

abstract class BPlusTreeNode<K extends Comparable<K>, V> implements Serializable {
  ArrayList<K> keys;
  int nodeSize;
  public UUID id; // for node management

  abstract V get(K key, TreeNodeManager<K, V> nodeManager);

  abstract void put(K key, V value, TreeNodeManager<K, V> nodeManager);

  abstract void remove(K key, TreeNodeManager<K, V> nodeManager);

  abstract boolean containsKey(K key, TreeNodeManager<K, V> nodeManager);

  abstract K getFirstLeafKey(); // TODO: check

  abstract BPlusTreeNode<K, V> split(TreeNodeManager<K, V> nodeManager);

  abstract void merge(BPlusTreeNode<K, V> sibling);

  int size() {
    return nodeSize;
  }

  boolean isOverFlow() {
    return nodeSize > Global.fanout - 1;
  }

  boolean isUnderFlow() {
    return nodeSize < (Global.fanout + 1) / 2 - 1;
  }

  int binarySearch(K key) {
    return Collections.binarySearch(keys.subList(0, nodeSize), key);
  }

  void keysAdd(int index, K key) {
    for (int i = nodeSize; i > index; i--) {
      keys.set(i, keys.get(i - 1));
    }
    keys.set(index, key);
    nodeSize++;
  }

  void keysRemove(int index) {
    for (int i = index; i < nodeSize - 1; i++) {
      keys.set(i, keys.get(i + 1));
    }
    nodeSize--;
  }

  public abstract void clear();
}
