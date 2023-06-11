package cn.edu.thssdb.schema;

import static cn.edu.thssdb.utils.FolderOperations.deleteFolder;
import static cn.edu.thssdb.utils.Global.DATA_DIR;

import cn.edu.thssdb.exception.AlreadyExistsException;
import cn.edu.thssdb.exception.FileException;
import cn.edu.thssdb.exception.NotExistsException;
import cn.edu.thssdb.query.MetaInfo;
import cn.edu.thssdb.query.QueryResult;
import cn.edu.thssdb.query.QueryTable;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Database implements Serializable {

  private String name;
  private transient HashMap<String, Table> tables;
  public HashMap<String, MetaInfo> metaInfos;
  transient ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

  private String path;
  private String metaPath;

  public Database(String name) {
    this.name = name;
    this.tables = new HashMap<>();
    this.metaInfos = new HashMap<>();
    this.path = DATA_DIR + name + "/";
    this.metaPath = this.path + "meta";
    recover();
  }

  public Table getTable(String tableName) {
    return tables.get(tableName);
  }

  public String getdatabaseName() {
    return name;
  }

  private void persist() {
    // save as file, when changes made
    File meta_file = new File(this.metaPath);
    if (!meta_file.exists()) { // create meta file if not exists -> USUALLY NOT HAPPEN
      try {
        meta_file.createNewFile();
      } catch (Exception e) {
        throw new FileException(FileException.Create, meta_file.getName());
      }
    }

    lock.writeLock().lock();
    try (FileOutputStream fileOut = new FileOutputStream(this.metaPath);
        ObjectOutputStream objectOut = new ObjectOutputStream(fileOut)) {
      objectOut.writeObject(this); // Serialize database object
    } catch (IOException e) {
      System.out.println("Database Metafile Serialization Failed!");
      System.out.println(e);
    } finally {
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
      tables.put(tableName, new Table(this.name, tableName, columns, true));
      // set folder path for node Manager
      metaInfos.put(tableName, new MetaInfo(tableName, new ArrayList<>(Arrays.asList(columns))));
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
    lock.writeLock().lock();
    try {
      mobj = metaInfos.get(tableName);
      tobj = tables.get(tableName);
      if (tobj == null) // table doesn't exist
      {
        throw new NotExistsException(NotExistsException.Table, tableName);
      }

      metaInfos.remove(tableName);
      tables.remove(tableName); // remove from HashMap if exists in it
      mobj = null;
      tobj = null;

      // delete corresponding file
      deleteFolder(new File(this.path + tableName));
      persist();
    } catch (Exception e) {
      tobj = null;
      mobj = null;
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
    File dbFolder = new File(this.path);
    if (!dbFolder.exists()) // Create Folder, if first create
    {
      lock.writeLock().lock();
      try {
        boolean created = dbFolder.mkdir();
        if (!created) throw new IOException();
      } catch (IOException e) {
        System.out.println("Database File Creation Failed!");
      } finally {
        lock.writeLock().unlock();
      }
    }

    File metaFile = new File(this.metaPath);
    if (metaFile.exists()) {
      lock.writeLock().lock();
      try (FileInputStream fileInputStream = new FileInputStream(this.metaPath);
          ObjectInputStream inputStream = new ObjectInputStream(fileInputStream); ) {
        Database restored = (Database) inputStream.readObject(); // read from file

        // recover
        if (restored != null) {
          this.name = restored.name;
          this.metaInfos = restored.metaInfos;

          // recover tables manually
          this.tables = new HashMap<>();
          for (MetaInfo info : this.metaInfos.values()) {
            List<Column> columnList = info.getColumns();
            Column[] array = columnList.toArray(new Column[0]);
            tables.put(
                info.getTableName(), new Table(this.name, info.getTableName(), array, false));
          }
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
    } else { // create meta file if it doesn't exist
      try {
        metaFile.createNewFile();
      } catch (Exception e) {
        throw new FileException(FileException.Create, metaFile.getName());
      }
    }
  }

  public void quit() {
    lock.writeLock().lock();
    try {
      persist();
      // TODO: wtf does this quit mean
    } catch (Exception e) {
      System.out.println(e);
    } finally {
      lock.writeLock().unlock();
    }
  }
}
