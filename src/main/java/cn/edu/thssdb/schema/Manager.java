package cn.edu.thssdb.schema;

import cn.edu.thssdb.exception.AlreadyExistsException;
import cn.edu.thssdb.exception.FileException;
import cn.edu.thssdb.exception.NotExistsException;

import java.io.*;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Manager {
  private HashMap<String, Database> databases;
  private Database curDB;
  private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

  public static Manager getInstance() {
    return Manager.ManagerHolder.INSTANCE;
  }

  public Manager() {
    /* TODO */
    // v1 done
    databases = new HashMap<>();
    loadData();
    // TODO
    curDB = null;
  }

  public Database getCurDB() {
    return curDB;
  }

  public void createDatabaseIfNotExists(String databaseName) {
    /* TODO */
    // v1 done
    if (databases.get(databaseName) != null) // exists already
    {
      throw new AlreadyExistsException(AlreadyExistsException.Database, databaseName);
    }

    Database newDB = new Database(databaseName);
    databases.put(databaseName, newDB);
    // 是否要persist?
    // 是否要加成功提示语？
  }

  public void deleteDatabase(String databaseName) {
    /* TODO */
    // v1 done
    if (databases.get(databaseName) == null)
      throw new NotExistsException(NotExistsException.Database, databaseName);

    databases.remove(databaseName);
    // 是否要persist?
    // 是否要加成功提示语？
  }

  public void switchDatabase(String databaseName) {
    /* TODO */
    // v1 done
    if (!databases.containsKey(databaseName))
      throw new NotExistsException(NotExistsException.Database, databaseName);

    curDB = getDB(databaseName);
    // 是否要persist?
    // 是否要加成功提示语？
  }

  private static class ManagerHolder {
    private static final Manager INSTANCE = new Manager();

    private ManagerHolder() {}
  }

  /* 自己添加的辅助方法 */

  private void loadData() {
    File data_dir = new File("data");
    if (!data_dir.exists()) // create directory if not exists
    data_dir.mkdir();
    File data_file = new File("data/" + "manager.data");
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
        while ((cur_line = reader.readLine()) != null) {
          createDatabaseIfNotExists(cur_line);
          // 目前没加readlog
        }
        reader.close();
      } catch (Exception e) {
        throw new FileException(FileException.ReadWrite, data_file.getName());
      }
    }
  }

  private void persist() {
    File data_file = new File("data/" + "manager.data");
    if (!data_file.exists()) { // create file if not exists
      try {
        data_file.createNewFile();
      } catch (Exception e) {
        throw new FileException(FileException.Create, data_file.getName());
      }
    }
    // write to file (from beginning)
    try {
      FileWriter writer = new FileWriter("data/" + "manager.data");
      for (String databaseName : databases.keySet()) {
        writer.write(databaseName);
        writer.write("\n");
      }
      writer.close();
    } catch (Exception e) {
      throw new FileException(FileException.ReadWrite, "manager.data");
    }
  }

  private Database getDB(String databaseName) {
    if (!databases.containsKey(databaseName))
      throw new NotExistsException(NotExistsException.Database, databaseName);
    return databases.get(databaseName);
  }
}
