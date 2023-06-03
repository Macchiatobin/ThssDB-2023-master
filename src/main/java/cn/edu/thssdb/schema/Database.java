package cn.edu.thssdb.schema;

import cn.edu.thssdb.exception.AlreadyExistsException;
import cn.edu.thssdb.exception.NotExistsException;
import cn.edu.thssdb.query.QueryResult;
import cn.edu.thssdb.query.QueryTable;

import java.io.*;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static cn.edu.thssdb.utils.FolderOperations.deleteFolder;
import static cn.edu.thssdb.utils.Global.DATA_DIR;

public class Database implements Serializable {

  // private static final long serialVersionUID = 1L;
  private String name;
  private HashMap<String, Table> tables;
  ReentrantReadWriteLock lock;

  private String path;

  public Database(String name) {
    this.name = name;
    this.tables = new HashMap<>();
    this.lock = new ReentrantReadWriteLock();
    this.path = DATA_DIR + name + "/";
    recover();
  }

  private void persist() {
    // save as file, when changes made

    // TODO: LOCK HANDLING
    String dbPath = DATA_DIR + this.name;
    File dbFolder = new File(dbPath);
    if (!dbFolder.exists()) // No database file
    {
      try {
        boolean created = dbFolder.mkdir();
        if (!created) throw new IOException();
      } catch (IOException e) {
        // TODO: error handling
        System.out.println("Database File Creation Failed!");
      }
    }

    String dbMetaPath = dbPath + "/meta";
    try (FileOutputStream fileOut = new FileOutputStream(dbMetaPath);
        ObjectOutputStream objectOut = new ObjectOutputStream(fileOut)) {
      objectOut.writeObject(this); // Serialize this object
    } catch (IOException e) {
      // TODO: error handling
      System.out.println("Database Metafile Serialization Failed!");
      System.out.println(e);
    }
  }

  // Create New Table
  public void create(String tableName, Column[] columns) {
    if (tables.get(tableName) != null) // table exists already
    {
      throw new AlreadyExistsException(AlreadyExistsException.Table, tableName);
    }
    tables.put(tableName, new Table(this.name, tableName, columns));
    persist();
    // TODO: do sth else
  }

  // Drop Table
  public void drop(String tableName) {
    Table obj = tables.get(tableName);
    if (obj == null) // table doesn't exist
    {
      throw new NotExistsException(NotExistsException.Table, tableName);
    }
    tables.remove(tableName);
    obj = null;
    deleteFolder(new File(path + tableName));
    persist();
  }

  public String select(QueryTable[] queryTables) {
    // TODO
    QueryResult queryResult = new QueryResult(queryTables);
    return null;
  }

  private void recover() {
    // TODO: read from data-file, when 'create'
    String filePath = DATA_DIR + this.name + "/meta";
    File metaFile = new File(filePath);
    if (metaFile.exists()) {
      try (FileInputStream fileInputStream = new FileInputStream(filePath);
          ObjectInputStream inputStream = new ObjectInputStream(fileInputStream); ) {
        Database restored = (Database) inputStream.readObject(); // read from file

        // recover
        if (restored != null) {
          this.name = restored.name;
          this.tables = restored.tables;
          this.lock = restored.lock;
        }
      } catch (IOException e) {
        // TODO: error handling
        System.out.println("InputStream Error Occurred During Recovering Database object!");
        System.out.println(e);
      } catch (ClassNotFoundException e) {
        System.out.println("ClassNotFoundError During Recovering Database object!");
        System.out.println(e);
      }
    }
  }

  // Added by Amy - 为ShowTable添加接口
  public Table getTable(String tableName) {
    return this.tables.get(tableName);
  }

  public void quit() {
    persist();
  }
}
