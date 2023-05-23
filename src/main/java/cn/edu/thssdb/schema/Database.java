package cn.edu.thssdb.schema;

import cn.edu.thssdb.query.QueryResult;
import cn.edu.thssdb.query.QueryTable;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Database {

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
    // TODO: save as file, when changes made
  }

  public void create(String tableName, Column[] columns) {
    if (tables.get(tableName) != null) //table exists already
    {
      System.out.println("Table exists already!");
      //TODO: do sth else
      return;
    }
    tables.put(tableName, new Table(this.name, tableName, columns));
    persist();
    //TODO: do sth else
  }

  public void drop(String tableName) {
    Table obj = tables.get(tableName);
    if (obj == null) //table exists already
    {
      System.out.println("Table doesn't exist!");
      //TODO: do sth else
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
  }

  public void quit() {
    // TODO
  }
}
