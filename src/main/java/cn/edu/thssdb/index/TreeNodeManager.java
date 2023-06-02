package cn.edu.thssdb.index;

import java.io.*;
import java.util.HashMap;
import java.util.UUID;

import static cn.edu.thssdb.utils.Global.CACHE_SIZE;

public class TreeNodeManager<K extends Comparable<K>, V> implements Serializable{

    private HashMap<K, UUID> idMap; // Key to id map, used for node loading, always PERSIST
    // UUID is file name for a node
    transient private HashMap<UUID, BPlusTreeNode<K, V>> cache = new HashMap<>();
    public K root_key; // recovered by Database.class

    public String path; // path for table folder, where node files are



    // only used when first creation of table.index
    public TreeNodeManager(BPlusTreeNode<K, V> root) {
        root_key = root.getFirstLeafKey(); // TODO: is this right?
        UUID root_id = UUID.randomUUID();
        idMap = new HashMap<>();
        idMap.put(root_key, root_id);
        cache.put(root.id, root); //cache should load root at the beginning
        //this.path is set by Database.class when creating table for the first time
        //just add uuid after that
        writeNodeToDisk(root_id, root);
    }

    public void recover() { // only used when recovering
        this.cache = new HashMap<>(); // recover transient
        loadNode(this.root_key);
        System.out.println("Node manager recovered!"); // THIS IS FOR DEBUGGING
    }

    private void checkCache() {
        if (cache.size() <= CACHE_SIZE) return; // no need to unload

        // cache full, unload one
        unloadNode(nodeToDeleteKey());
    }

    private K nodeToDeleteKey() {
        // TODO: LRU?
        return null;
    }

    public BPlusTreeNode<K, V> loadNode(K key) {
        if (cache.containsKey(idMap.get(key))) { // cache hit
            return cache.get(idMap.get(key));
        }


        checkCache(); // check cache first
        // read from disk
        BPlusTreeNode<K, V> node = readNodeFromDisk(idMap.get(key));
        cache.put(node.id, node);
        return node;
    }

    // called when cache is full and new node is needed
    void unloadNode(K key) {
        writeNodeToDisk(idMap.get(key), cache.get(idMap.get(key)));
    }

    private BPlusTreeNode<K, V> readNodeFromDisk(UUID id) {
        File file = new File(this.path + id.toString()); // node file
        try (FileInputStream fileInputStream = new FileInputStream(file);
             ObjectInputStream inputStream = new ObjectInputStream(fileInputStream)) {
            BPlusTreeNode restored = (BPlusTreeNode) inputStream.readObject();
            return restored;
        } catch (Exception e) {
            System.out.println(e);
        }
        return null; // TODO: check if i should return null here
    }

    //PERSIST node
    public void writeNodeToDisk(UUID id, BPlusTreeNode<K, V> node) {
        File nodeFile = new File(this.path + id.toString());
        if (!nodeFile.exists()) {
            // create file if not exists
            // should only be triggered when a node is created for the first time
            try {
                nodeFile.createNewFile();
                // uuid for new node is created in upper class
                idMap.put(node.keys.get(0), id); // need to be put into idMap when created
            } catch (Exception e) {
                // maybe some IO exception
                System.out.println(e);
            }
        }

        try (FileOutputStream fileOut = new FileOutputStream(nodeFile);
             ObjectOutputStream objectOut = new ObjectOutputStream(fileOut)) {
            objectOut.writeObject(node);
        } catch (Exception e) {
            // may be some file not found exception
            System.out.println(e);
        }
    }
}
