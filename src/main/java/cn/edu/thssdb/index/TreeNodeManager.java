package cn.edu.thssdb.index;

import java.io.*;
import java.util.*;

import static cn.edu.thssdb.utils.FolderOperations.deleteFolder;
import static cn.edu.thssdb.utils.Global.*;

public class TreeNodeManager<K extends Comparable<K>, V> implements Serializable {

  // private HashMap<K, UUID> idMap; // Key to id map, used for node loading, always PERSIST
  // UUID is file name for a node
  private transient HashMap<UUID, BPlusTreeNode<K, V>> cache = new HashMap<>();
  private transient LinkedHashMap<UUID, BPlusTreeNode<K, V>> accessOrderCache;
  public UUID root_id; // recovered by deserialization of table

  public String path; // path for table folder, where node files are, set when table created

  // only used when first creation of table.index
  public TreeNodeManager(BPlusTreeNode<K, V> root) {
    this.root_id = root.id;
    this.cache = new HashMap<>();
    this.accessOrderCache = new LinkedHashMap<>(16, 0.75f, true); // order by access order
    // idMap = new HashMap<>();
    // idMap.put(root_key, root.id);

    cache.put(root.id, root); // cache should load root at the beginning
    accessOrderCache.put(root.id, root);

    // this.path is set by Database.class when creating table for the first time
    // just add uuid after that

    writeNodeToDisk(root);
  }

  // called by Database.class
  // recreate new cache and load root to cache
  public void recover() { // only used when recovering
    this.cache = new HashMap<>(); // recover transient
    this.accessOrderCache = new LinkedHashMap<>(16, 0.75f, true); // order by access
    loadNode(this.root_id);

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

    // this.idMap.put(key, id);

    this.cache.put(id, node);
    this.accessOrderCache.put(id, node);
    // writing to disk should be done manually by caller,
    // after modifying more details about new node

    return node;
  }

  // delete node method
  // should be called after managing all changes that could be made to its
  // parents/siblings/childrens
  public void deleteNode(UUID id) {
    if (id == null) return;
    if (cache.containsKey(id)) { // if it's in cache (it will be in cache if it's root node)
      cache.remove(id);
      accessOrderCache.remove(id);
    }

    // this.idMap.remove(key);
    deleteFolder(new File(this.path + id.toString()));
  }

  private void checkCache() {
    if (cache.size() <= CACHE_SIZE) return; // no need to unload

    // cache full, unload one
    unloadNode(nodeToUnloadId());
  }

  private UUID nodeToUnloadId() {
    Iterator<UUID> iterator = accessOrderCache.keySet().iterator();
    while (iterator.hasNext()) {
      UUID id = iterator.next();
      if (!id.equals(root_id)) {
        return id;
      }
    }
    return null;
  }

  public BPlusTreeNode<K, V> loadNode(UUID id) {
    if (id == null) return null;

    if (cache.containsKey(id)) { // cache hit
      return cache.get(id);
    }

    checkCache(); // check cache first
    // read from disk
    BPlusTreeNode<K, V> node = readNodeFromDisk(id);
    cache.put(node.id, node);
    accessOrderCache.put(node.id, node);
    return node;
  }

  // called when cache is full and new node is needed
  void unloadNode(UUID id) {
    writeNodeToDisk(cache.get(id)); // persist in case
    cache.remove(id); // unload
    accessOrderCache.remove(id);
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

  // PERSIST node
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
