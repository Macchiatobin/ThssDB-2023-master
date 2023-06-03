package cn.edu.thssdb.index;

import java.io.*;
import java.util.HashMap;
import java.util.UUID;

import static cn.edu.thssdb.utils.FolderOperations.deleteFolder;
import static cn.edu.thssdb.utils.Global.CACHE_SIZE;
import static cn.edu.thssdb.utils.Global.INTERNAL;

public class TreeNodeManager<K extends Comparable<K>, V> implements Serializable {

    //private HashMap<K, UUID> idMap; // Key to id map, used for node loading, always PERSIST
    // UUID is file name for a node
    transient private HashMap<UUID, BPlusTreeNode<K, V>> cache = new HashMap<>();
    public UUID root_id; // recovered by deserialization of table

    public String path; // path for table folder, where node files are



    // only used when first creation of table.index
    public TreeNodeManager(BPlusTreeNode<K, V> root) {
        this.root_id = root.id;
        this.cache = new HashMap<>();
        //idMap = new HashMap<>();
        //idMap.put(root_key, root.id);

        cache.put(root.id, root); //cache should load root at the beginning

        //this.path is set by Database.class when creating table for the first time
        //just add uuid after that

        writeNodeToDisk(root);
    }

    // called by Database.class
    // recreate new cache and load root to cache
    public void recover() { // only used when recovering
        this.cache = new HashMap<>(); // recover transient
        loadNode(this.root_id);
        // TODO: root is already recovered when recovering Table.class
        // TODO: check if we need only cache.put, if operations on root is as expected if we do loadNode

        System.out.println("Node manager recovered!"); // THIS IS FOR DEBUGGING
    }

    // new node method
    // called when creating new node in tree
    // returns created node with key[0], id, type all set
    public BPlusTreeNode<K, V> newNode(int size, int node_type, K first_key, UUID parent_id) {
        checkCache(); // make space in cache first

        BPlusTreeNode<K, V> node;

        if (node_type == INTERNAL) node = new BPlusTreeInternalNode<>(size, parent_id);
        else node = new BPlusTreeLeafNode<>(size, parent_id);

        // set idMap, cache and write to Disk

        UUID id = UUID.randomUUID();
        node.keys.set(0, first_key);
        node.id = id;

        //this.idMap.put(key, id);

        this.cache.put(id, node);
        // writing to disk should be done manually by caller,
        // after modifying more details about new node

        return node;
    }

    // delete node method
    // should be called after managing all changes that could be made to its parents/siblings/childrens
    public void deleteNode(UUID id) { // TODO: node as parameter?
        if (cache.containsKey(id)) { // if it's in cache (it will be in cache if it's root node)
            cache.remove(id);
        }

        //this.idMap.remove(key);
        deleteFolder(new File(this.path + id.toString()));
    }

    private void checkCache() {
        if (cache.size() <= CACHE_SIZE) return; // no need to unload

        // cache full, unload one
        unloadNode(nodeToDeleteId());
    }

    private UUID nodeToDeleteId() {
        // TODO: LRU? return id of node to unload from cache
        // should never unload root node
        return null;
    }

    public BPlusTreeNode<K, V> loadNode(UUID id) {
        if (cache.containsKey(id)) { // cache hit
            return cache.get(id);
        }


        checkCache(); // check cache first
        // read from disk
        BPlusTreeNode<K, V> node = readNodeFromDisk(id);
        cache.put(node.id, node);
        return node;
    }

    // called when cache is full and new node is needed
    void unloadNode(UUID id) {
        writeNodeToDisk(cache.get(id)); // persist
        // TODO: check if persisting needed
        cache.remove(id); // unload
    }

    private BPlusTreeNode<K, V> readNodeFromDisk(UUID id) {
        File file = new File(this.path + id.toString()); // node file
        try (FileInputStream fileInputStream = new FileInputStream(file);
             ObjectInputStream inputStream = new ObjectInputStream(fileInputStream)) {
            BPlusTreeNode restored = (BPlusTreeNode) inputStream.readObject();
            return restored;
        } catch (Exception e) {
            System.out.println(e); // maybe some file not found exception
        }
        return null; // TODO: check if i should return null here
    }

    //PERSIST node
    public void writeNodeToDisk(BPlusTreeNode<K, V> node) {
        File nodeFile = new File(this.path + node.id.toString());
        if (!nodeFile.exists()) {
            // create file if not exists
            // should only be triggered when a node is created for the first time
            try {
                nodeFile.createNewFile();
                // uuid for new node is created in upper class
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
