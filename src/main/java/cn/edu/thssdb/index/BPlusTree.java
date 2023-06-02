package cn.edu.thssdb.index;

import cn.edu.thssdb.utils.Pair;

import java.io.Serializable;
import java.util.UUID;

public final class BPlusTree<K extends Comparable<K>, V>
    implements Iterable<Pair<K, V>>, Serializable {

  transient public BPlusTreeNode<K, V> root;
  private int size;
  public TreeNodeManager<K, V> nodeManager;

  // only called when create table
  public BPlusTree() {
    root = new BPlusTreeLeafNode<>(0);
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
    if (root instanceof BPlusTreeInternalNode && root.size() == 0) {
      // root = ((BPlusTreeInternalNode<K, V>) root).children.get(0);

      // modify start
      root = this.nodeManager.loadNode(((BPlusTreeInternalNode<K, V>) root).children.get(0));
      // load first children and make it root
      this.nodeManager.root_key = root.keys.get(0); // TODO: root_key should be first key?
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
      BPlusTreeNode<K, V> newSiblingNode = root.split(this.nodeManager);
      // disk write and cache load is done in 'split'

      BPlusTreeInternalNode<K, V> newRoot = new BPlusTreeInternalNode<>(1);

      //modify start
      UUID newRootId = UUID.randomUUID();
      newRoot.id = newRootId;
      //modify end

      newRoot.keys.set(0, newSiblingNode.getFirstLeafKey());
      //newRoot.children.set(0, root);
      newRoot.children.set(0, root.keys.get(0));
      //newRoot.children.set(1, newSiblingNode);
      newRoot.children.set(1, newSiblingNode.keys.get(0));
      root = newRoot;

      //modify start
      K newRootKey = newRoot.keys.get(0);
      this.nodeManager.root_key = newRootKey;
      this.nodeManager.idMap.put(newRootKey, newRootId); // TODO: maybe method for inserting new node needed in manager
      //modify end
      // TODO: should put root id to manager
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
