package cn.edu.thssdb.index;

import java.util.HashMap;

public class TreeNodeManager<K extends Comparable<K>, V> {
    private HashMap<K, Integer> idMap; // Key to id map, used for node loading
    transient private HashMap<K, BPlusTreeNode<K, V>> cache = new HashMap<>();
    public int root_id;

    public TreeNodeManager(BPlusTreeNode<K, V> node) { // only used when first creation of table.index
        root_id = 0; // TODO: maybe there would be better initial value
        idMap = new HashMap<>();
        cache.put(node.getFirstLeafKey(), node); //cache should load root at the beginning
    }

    public BPlusTreeNode<K, V> loadNode(K key) {
        if (cache.containsKey(key)) {
            //idMap.put(key, idMap.get(key) + 1);
            return cache.get(key);
        }
        BPlusTreeNode<K, V> node = readNodeFromDisk(idMap.get(key));
        cache.put(key, node);
        //idMap.put(key, 1);
        return node;
    }

    // 卸载节点
    void unloadNode(K key) {
        /*
        int refCount = idMap.get(key);
        if (refCount == 1) {
            writeNodeToDisk(cache.get(key));
            cache.remove(key);
        }
        idMap.put(key, refCount - 1);
         */
        writeNodeToDisk(cache.get(key));
        // TODO: is this right?
    }

    // 把节点读取自外部存储
    private BPlusTreeNode<K, V> readNodeFromDisk(int id) {
        // TODO
        // 在这里使用序列化/反序列化
        return null;
    }

    // 把节点写入到外部存储
    private void writeNodeToDisk(BPlusTreeNode<K, V> node) {
        // TODO
        // 在这里使用序列化/反序列化
    }
}
