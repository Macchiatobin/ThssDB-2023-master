package cn.edu.thssdb.schema;

import cn.edu.thssdb.query.QueryResult;
import cn.edu.thssdb.query.QueryTable;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static cn.edu.thssdb.utils.Global.DATA_DIR;

public class Database implements Serializable {

  private static final long serialVersionUID = 1L;
  private String name;
  private HashMap<String, Table> tables;
  ReentrantReadWriteLock lock;

  public Database(String name) {
    this.name = name;
    this.tables = new HashMap<>();
    this.lock = new ReentrantReadWriteLock();
    recover();
  }

  private void persist() {
    //save as file, when changes made
    //TODO: LOCK HANDLING
    String dbPath = DATA_DIR + this.name;
    File dbFolder = new File(dbPath);
    if (!dbFolder.exists()) //No database file
    {
      try {
        boolean created = dbFolder.mkdir();
        if (!created) throw new IOException();
      }
      catch (IOException e)
      {
        //TODO: error handling
        System.out.println("Database File Creation Failed!");
      }
    }



  }

  public void create(String tableName, Column[] columns) {
    if (tables.get(tableName) != null) // table exists already
    {
      System.out.println("Table exists already!");
      // TODO: do sth else
      return;
    }
    tables.put(tableName, new Table(this.name, tableName, columns));
    persist();
    // TODO: do sth else
  }

  public void drop(String tableName) {
    Table obj = tables.get(tableName);
    if (obj == null) // table exists already
    {
      System.out.println("Table doesn't exist!");
      // TODO: do sth else
      return;
    }
    tables.remove(tableName);
    obj = null;
    persist();
  }

  public String select(QueryTable[] queryTables) {
    // TODO
    QueryResult queryResult = new QueryResult(queryTables);
    return null;
  }

  private void recover() {
    // TODO: read from data-file, when 'create'
    // Gotta recover tables of current database first?
    for (String key : tables.keySet())
    {
      Table curTable = tables.get(key);
      //curTable.recover();
    }
  }

  public void quit() {
    // TODO
  }
}
