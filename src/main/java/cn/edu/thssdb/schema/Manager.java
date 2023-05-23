package cn.edu.thssdb.schema;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Manager {
  private HashMap<String, Database> databases;
  private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

  public static Manager getInstance() {
    return Manager.ManagerHolder.INSTANCE;
  }

  public Manager() {
    // TODO
  }

  private void createDatabaseIfNotExists(String databaseName) {
    /* macchiato code start */
    if (databases.get(databaseName) != null) // exists already
    {
      System.out.println("Database exists already!");
      return;
      //TODO: 修改报错形式以及返回状态
    }

    Database newDB = new Database(databaseName);
    databases.put(databaseName, newDB);
    /* macchiato code end */

    // TODO: 应该返回成功状态什么的...
  }

  private void deleteDatabase(String databaseName) {
    /* macchiato code start */
    if (databases.get(databaseName) == null)
    {
      System.out.println("Database not existing");
      return;
      //TODO: 修改报错形式以及返回状态
    }

    databases.remove(databaseName);
    /* macchiato code end */

    //TODO
  }

  public void switchDatabase() {
    // TODO
  }

  private static class ManagerHolder {
    private static final Manager INSTANCE = new Manager();

    private ManagerHolder() {}
  }
}
