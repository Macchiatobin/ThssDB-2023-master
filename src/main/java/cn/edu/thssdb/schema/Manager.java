package cn.edu.thssdb.schema;

import cn.edu.thssdb.exception.AlreadyExistsException;
import cn.edu.thssdb.exception.FileException;
import cn.edu.thssdb.exception.NotExistsException;

import java.io.*;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static cn.edu.thssdb.utils.FolderOperations.deleteFolder;
import static cn.edu.thssdb.utils.Global.DATA_DIR;

public class Manager implements Serializable {
  private HashMap<String, Database> databases;
  private Database curDB;
  private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private String filePath = DATA_DIR + "manager/";

  public static Manager getInstance() {
    return Manager.ManagerHolder.INSTANCE;
  }

  private Manager() {
    /* TODO */
    // v1 done
    databases = new HashMap<>();
    curDB = null;
    loadData();
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
    persist();
  }

  public void deleteDatabase(String databaseName) {
    /* TODO */
    // v1 done
    if (databases.get(databaseName) == null)
      throw new NotExistsException(NotExistsException.Database, databaseName);

    databases.remove(databaseName);
    String folderPath = DATA_DIR + databaseName;
    File folder = new File(folderPath);
    deleteFolder(folder);
    persist();
  }

  public void switchDatabase(String databaseName) {
    /* TODO */
    // v1 done
    if (!databases.containsKey(databaseName))
      throw new NotExistsException(NotExistsException.Database, databaseName);

    databases.put(
        databaseName, new Database(databaseName)); // Load database(will read from file if exists)
    curDB = getDB(databaseName);
  }

  // 单例模式
  private static class ManagerHolder implements Serializable {
    private static final Manager INSTANCE = new Manager();

    private ManagerHolder() {}
  }

  private void loadData() {
    File data_dir = new File(DATA_DIR);
    if (!data_dir.exists()) // create directory if not exists
    data_dir.mkdir();
    File data_file = new File(filePath);
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
          // modified from here
          databases.put(cur_line, null);
          // modify ended

          // createDatabaseIfNotExists(cur_line); //original
          // 目前没加readlog
        }
        reader.close();
      } catch (Exception e) {
        throw new FileException(FileException.ReadWrite, data_file.getName());
      }
    }
  }

  private void persist() {
    File data_file = new File(filePath);
    if (!data_file.exists()) { // create file if not exists
      try {
        data_file.createNewFile();
      } catch (Exception e) {
        throw new FileException(FileException.Create, data_file.getName());
      }
    }
    // write to file (from beginning), only database names
    try {
      FileWriter writer = new FileWriter(filePath);
      for (String databaseName : databases.keySet()) {
        writer.write(databaseName);
        writer.write("\n");
      }
      writer.close();
    } catch (Exception e) {
      throw new FileException(FileException.ReadWrite, filePath);
    }
  }

  private Database getDB(String databaseName) {
    if (!databases.containsKey(databaseName))
      throw new NotExistsException(NotExistsException.Database, databaseName);
    return databases.get(databaseName);
  }
}
