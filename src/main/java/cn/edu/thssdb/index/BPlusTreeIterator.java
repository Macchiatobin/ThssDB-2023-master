package cn.edu.thssdb.index;

import cn.edu.thssdb.utils.Pair;

import java.util.Iterator;
import java.util.LinkedList;

public class BPlusTreeIterator<K extends Comparable<K>, V> implements Iterator<Pair<K, V>> {
  private final LinkedList<BPlusTreeNode<K, V>> queue;
  private final LinkedList<Pair<K, V>> buffer;
  private TreeNodeManager<K, V> nodeManager;

  BPlusTreeIterator(BPlusTree<K, V> tree, TreeNodeManager<K, V> nodeManager) {
    queue = new LinkedList<>();
    buffer = new LinkedList<>();
    if (tree.size() == 0) return;
    queue.add(tree.root);
    this.nodeManager = nodeManager;
  }

  @Override
  public boolean hasNext() {
    return !queue.isEmpty() || !buffer.isEmpty();
  }

  @Override
  public Pair<K, V> next() {
    // buffer is (entry,row)s
    // queue is internal nodes to be search down
    if (buffer.isEmpty()) {
      while (true) {
        BPlusTreeNode<K, V> node = queue.poll();
        if (node instanceof BPlusTreeLeafNode) {
          for (int i = 0; i < node.size(); i++)
            buffer.add( // add key-value(row)s
                new Pair<>(node.keys.get(i), ((BPlusTreeLeafNode<K, V>) node).values.get(i)));
          break;
        } else if (node instanceof BPlusTreeInternalNode)
          for (int i = 0; i <= node.size(); i++) {
            // queue.add(((BPlusTreeInternalNode<K, V>) node).children.get(i));
            queue.add(
                this.nodeManager.loadNode(((BPlusTreeInternalNode<K, V>) node).children_id.get(i)));
          }
      }
    }
    return buffer.poll(); // first value in buffer
  }
}
