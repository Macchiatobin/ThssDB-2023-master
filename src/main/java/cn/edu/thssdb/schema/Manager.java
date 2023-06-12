package cn.edu.thssdb.schema;

import static cn.edu.thssdb.utils.FolderOperations.deleteFolder;
import static cn.edu.thssdb.utils.Global.DATA_DIR;

import cn.edu.thssdb.exception.AlreadyExistsException;
import cn.edu.thssdb.exception.FileException;
import cn.edu.thssdb.exception.NotExistsException;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Manager implements Serializable {
  private HashMap<String, Database> databases;
  private Database curDB;
  private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private String metaPath = DATA_DIR + "manager_meta/";

  // Transaction Lock
  public HashMap<Long, ArrayList<String>> readLockMap; // 记录每个session取得了哪些表的s锁
  public HashMap<Long, ArrayList<String>> writeLockMap; // 记录每个session取得了哪些表的x锁
  public ArrayList<Long> inTransactionList; // 处于transaction状态的session列表
  public ArrayList<Long> lockTransactionList; // 由于锁阻塞的session队列

  public static Manager getInstance() {
    return Manager.ManagerHolder.INSTANCE;
  }

  private Manager() {
    databases = new HashMap<>();
    curDB = null;
    loadData(); // recover
    readLockMap = new HashMap<>();
    writeLockMap = new HashMap<>();
    inTransactionList = new ArrayList<>();
    lockTransactionList = new ArrayList<>();
  }

  public Database getCurDB() {
    return curDB;
  }

  public void createDatabaseIfNotExists(String databaseName) {
    /* TODO */
    // v1 done
    lock.writeLock().lock();
    try {

      if (databases.get(databaseName) != null) // exists already
      {
        throw new AlreadyExistsException(AlreadyExistsException.Database, databaseName);
      }
      Database newDB = new Database(databaseName);
      databases.put(databaseName, newDB);
      persist();
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void deleteDatabase(String databaseName) {
    // v1 done
    lock.writeLock().lock();
    try {
      if (databases.get(databaseName) == null)
        throw new NotExistsException(NotExistsException.Database, databaseName);
      databases.remove(databaseName);
      String folderPath = DATA_DIR + databaseName;
      deleteFolder(new File(folderPath));
      persist();
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void switchDatabase(String databaseName) {
    /* TODO */
    // v1 done
    lock.readLock().lock();
    try {
      if (!databases.containsKey(databaseName))
        throw new NotExistsException(NotExistsException.Database, databaseName);
      curDB = getDB(databaseName);
    } finally {
      lock.readLock().unlock();
    }
  }

  // 单例模式
  private static class ManagerHolder implements Serializable {
    private static final Manager INSTANCE = new Manager();

    private ManagerHolder() {}
  }

  private void loadData() {
    File data_dir = new File(DATA_DIR);
    if (!data_dir.exists()) data_dir.mkdir(); // create directory if not exists
    File data_file = new File(metaPath);
    if (!data_file.exists()) { // create file if not exists
      try {
        data_file.createNewFile();
      } catch (Exception e) {
        throw new FileException(FileException.Create, data_file.getName());
      }
    } else { // read from existing file
      try {
        BufferedReader reader = new BufferedReader(new FileReader(data_file));
        String cur_line = null;
        while ((cur_line = reader.readLine()) != null) { // cur_line is databaseName
          databases.put(cur_line, new Database(cur_line)); // load databases
        }
        reader.close();
      } catch (Exception e) {
        throw new FileException(FileException.ReadWrite, data_file.getName());
      }
    }
  }

  private void persist() { // only persists names of databases
    File data_file = new File(metaPath);
    if (!data_file.exists()) { // create file if not exists
      try {
        data_file.createNewFile();
      } catch (Exception e) {
        throw new FileException(FileException.Create, data_file.getName());
      }
    }

    // write to file (from beginning), only database names
    try {
      FileWriter writer = new FileWriter(metaPath);
      for (String databaseName : databases.keySet()) {
        writer.write(databaseName);
        writer.write("\n");
      }
      writer.close();
    } catch (Exception e) {
      throw new FileException(FileException.ReadWrite, metaPath);
    }
  }

  private Database getDB(String databaseName) {
    lock.readLock().lock();
    try {
      if (!databases.containsKey(databaseName))
        throw new NotExistsException(NotExistsException.Database, databaseName);
      return databases.get(databaseName);
    } finally {
      lock.readLock().unlock();
    }
  }
}
