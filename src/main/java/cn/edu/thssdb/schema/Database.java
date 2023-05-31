package cn.edu.thssdb.schema;

import cn.edu.thssdb.exception.AlreadyExistsException;
import cn.edu.thssdb.exception.NotExistsException;
import cn.edu.thssdb.query.MetaInfo;
import cn.edu.thssdb.query.QueryResult;
import cn.edu.thssdb.query.QueryTable;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static cn.edu.thssdb.utils.FolderOperations.deleteFolder;
import static cn.edu.thssdb.utils.Global.DATA_DIR;

public class Database implements Serializable {

  private String name;
  transient private HashMap<String, Table> tables;
  public HashMap<String, MetaInfo> metaInfos;
  transient ReentrantReadWriteLock lock;

  private String path;

  public Database(String name) {
    this.name = name;
    this.tables = new HashMap<>();
    this.metaInfos = new HashMap<>();
    this.path = DATA_DIR + name + "/";
    this.lock = new ReentrantReadWriteLock();
    recover();
  }

  public Table getTable(String tableName) {
    return tables.get(tableName);
  }

  private void persist() {
    // save as file, when changes made
    String dbPath = DATA_DIR + this.name;
    File dbFolder = new File(dbPath);
    if (!dbFolder.exists()) // No database file
    {
      lock.writeLock().lock();
      try {
        boolean created = dbFolder.mkdir();
        if (!created) throw new IOException();
      } catch (IOException e) {
        // TODO: error handling
        System.out.println("Database File Creation Failed!");
      }
      finally
      {
        lock.writeLock().unlock();
      }
    }

    String dbMetaPath = dbPath + "/meta";
    lock.writeLock().lock();
    try (FileOutputStream fileOut = new FileOutputStream(dbMetaPath);
        ObjectOutputStream objectOut = new ObjectOutputStream(fileOut)) {
      objectOut.writeObject(this); // Serialize this object
    } catch (IOException e) {
      System.out.println("Database Metafile Serialization Failed!");
      System.out.println(e);
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  // Create New Table
  public void create(String tableName, Column[] columns) {
    if (tables.get(tableName) != null) // table exists already
    {
      throw new AlreadyExistsException(AlreadyExistsException.Table, tableName);
    }

    lock.writeLock().lock();
    try {
      tables.put(tableName, new Table(this.name, tableName, columns));
      metaInfos.put(tableName, new MetaInfo(this.name, new ArrayList<>(Arrays.asList(columns))));
      persist();
    } catch (Exception e) {
      System.out.println(e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  // Drop Table
  public void drop(String tableName) { // drop table and delete files
    Table tobj;
    MetaInfo mobj;
    lock.readLock().lock();
    try {
      mobj = metaInfos.get(tableName);
      tobj = tables.get(tableName);

      metaInfos.remove(tableName);
      if (tobj != null) tables.remove(tableName); // remove from HashMap if exists in it

      // delete corresponding file
      deleteFolder(new File(this.path + tableName));

    } catch (Exception e) {
      tobj = null;
      mobj = null;

      System.out.println(e);
    } finally {
      lock.readLock().unlock();
    }

    if (tobj == null) // table doesn't exist
    {
      throw new NotExistsException(NotExistsException.Table, tableName);
    }

    lock.writeLock().lock();
    try {
      tables.remove(tableName);
      metaInfos.remove(tableName);
      tobj = null;
      mobj = null;
      deleteFolder(new File(path + tableName));
      persist();
    } catch (Exception e) {
      System.out.println(e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  public String select(QueryTable[] queryTables) {
    // TODO
    QueryResult queryResult = new QueryResult(queryTables);
    return null;
  }

  private void recover() { // read from file, when create
    String filePath = DATA_DIR + this.name + "/meta";

    File metaFile = new File(filePath);
    if (metaFile.exists()) {
      lock.writeLock().lock();
      try (FileInputStream fileInputStream = new FileInputStream(filePath);
          ObjectInputStream inputStream = new ObjectInputStream(fileInputStream); ) {
        Database restored = (Database) inputStream.readObject(); // read from file

        // recover
        if (restored != null) {
          this.name = restored.name;
          //this.tables = restored.tables;
          this.metaInfos = restored.metaInfos;

          // TODO: serialize tables from metaInfos?
        }
      } catch (IOException e) {
        System.out.println("InputStream Error Occurred During Recovering Database object!");
        System.out.println(e);
      } catch (ClassNotFoundException e) {
        System.out.println("ClassNotFoundError During Recovering Database object!");
        System.out.println(e);
      } finally {
        lock.writeLock().unlock();
      }
    }
  }

  public void quit() {
    lock.writeLock().lock();
    try {
      persist();
      // TODO: table persist?
    } catch (Exception e) {
      System.out.println(e);
    } finally {
      lock.writeLock().unlock();
    }

  }
}
