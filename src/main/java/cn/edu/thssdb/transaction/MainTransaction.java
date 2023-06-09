package cn.edu.thssdb.transaction;

import cn.edu.thssdb.exception.NotExistsException;
import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.plan.impl.*;
import cn.edu.thssdb.schema.Column;
import cn.edu.thssdb.schema.Logger;
import cn.edu.thssdb.schema.Manager;
import cn.edu.thssdb.schema.Table;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MainTransaction implements Serializable {
  private Manager manager;
  private String databaseName;
  private Logger logger; // 日志对象
  private ReentrantReadWriteLock transactionLock;

  private HashMap<String, ReentrantReadWriteLock.ReadLock> readLocks;
  private HashMap<String, ReentrantReadWriteLock.WriteLock> writeLocks;
  private LinkedList<LogicalPlan> planList;
  private boolean checkTransaction = false;
  private List<Column> columns;

  public MainTransaction(String databaseName, Logger logger) {
    this.manager = Manager.getInstance();
    this.databaseName = databaseName;
    this.logger = logger;
    this.transactionLock = new ReentrantReadWriteLock();
    this.readLocks = new HashMap<>();
    this.writeLocks = new HashMap<>();
    this.planList = new LinkedList<>();
  }

  public TransactionFlag exec(LogicalPlan plan) {
    System.out.println("Main Transaction Plan:" + plan);
    if (plan instanceof SelectPlan) {
      System.out.println("Trans Select");
      return readTransaction(plan);
    } else if (plan instanceof UpdatePlan
        || plan instanceof DeletePlan
        || plan instanceof InsertPlan) {
      System.out.println("Trans I?U?D");
      return writeTransaction(plan);
    } else if (plan instanceof CommitPlan) {
      System.out.println("Trans Commit");
      return commitTransaction();
    } else if (plan instanceof BeginTransactionPlan) {
      System.out.println("Trans Begin");
      return beginTransaction();
    } else {
      System.out.println("Unknown");
      return endTransaction(plan);
    }
  }

  public TransactionFlag exec(LogicalPlan plan, LinkedList<String> log) {
    System.out.println("Main Transaction Plan:" + plan);
    logger.writeLines(log);
    if (plan instanceof SelectPlan) {
      System.out.println("Trans Select");
      return readTransaction(plan);
    } else if (plan instanceof UpdatePlan
        || plan instanceof DeletePlan
        || plan instanceof InsertPlan) {
      System.out.println("Trans I?U?D");
      return writeTransaction(plan);
    } else if (plan instanceof CommitPlan) {
      System.out.println("Trans Commit");
      return commitTransaction();
    } else if (plan instanceof BeginTransactionPlan) {
      System.out.println("Trans Begin");
      return beginTransaction();
    } else {
      System.out.println("Unknown");
      return endTransaction(plan);
    }
  }

  private TransactionFlag endTransaction(LogicalPlan plan) {
    System.out.println("endT check:" + checkTransaction);
    if (checkTransaction) {
      commitTransaction();
    }
    try {
      // 检查删除的数据库此时是否被占用
      //      if (operation instanceof DropDatabasePlan) {
      //        for (Table table : Manager.getInstance().getCurDB(((DropDatabasePlan) operation).)
      //                .getTables()) {
      //          if (table.lock.isWriteLocked())
      //            throw new DatabaseOccupiedException();
      //        }
      //      }
      //      plan.execute_plan();
      //      if (plan instanceof CreateTablePlan || plan instanceof DropTablePlan) {
      //        logger.writeLines(plan.getLog());
      //      }
      checkTransaction = false;
    } catch (Exception e) {
      System.out.println("endT:" + e);
      return new TransactionFlag(false, e.getMessage());
    }
    System.out.println("endT: Success");
    return new TransactionFlag(true, "Success");
  }

  private TransactionFlag beginTransaction() {
    System.out.println("beginT check:" + checkTransaction);
    if (databaseName == null) {
      throw new NotExistsException(1, "");
    }
    if (checkTransaction) {
      return new TransactionFlag(false, "Other plan is working");
    } else {
      checkTransaction = true;
      LinkedList<String> log = new LinkedList<>();
      log.add("BEGIN TRANSACTION");
      logger.writeLines(log);
      return new TransactionFlag(true, "Success");
    }
  }

  private TransactionFlag commitTransaction() {
    System.out.println("commitT check:" + checkTransaction);

    if (databaseName == null) {
      throw new NotExistsException(1, "");
    }

    try {
      transactionLock.writeLock().lock();
      LinkedList<String> log = new LinkedList<>();
      while (!planList.isEmpty()) {
        LogicalPlan lp = planList.getFirst();
        // log.addAll(lp.getLog());
        planList.removeFirst();
      }
      log.add("COMMIT");
      logger.writeLines(log);
      checkTransaction = false;
      return new TransactionFlag(true, "Success");
    } catch (NullPointerException e) {
      return new TransactionFlag(false, "No pending operations to commit");
    } finally {
      transactionLock.writeLock().unlock();
      releaseAllLocks();
    }
  }

  private TransactionFlag readTransaction(LogicalPlan plan) {
    System.out.println("readT check:" + checkTransaction);

    try {
      transactionLock.readLock().lock();

      if (checkTransaction) {
        return new TransactionFlag(false, "Other transaction is in progress");
      }

      // Check if there are any pending write operations in the planList
      if (!planList.isEmpty()) {
        return new TransactionFlag(false, "Pending write operations, cannot read");
      }

      String tableName = String.valueOf(plan.getTableName()); // 获取表名
      System.out.println("read tableName e:" + tableName);
      ReentrantReadWriteLock.ReadLock readLock = getReadLock(tableName);
      System.out.println("read readLock e:" + readLock);

      readLock.lock(); // 加读锁
      return new TransactionFlag(true, "Success");
    } catch (Exception e) {
      System.out.println("readT e:" + e);
      return new TransactionFlag(false, e.getMessage());
    } finally {
      transactionLock.readLock().unlock();
    }
  }

  private TransactionFlag writeTransaction(LogicalPlan plan) {
    System.out.println("writeT check:" + checkTransaction);

    try {
      transactionLock.writeLock().lock();
      String tableName = String.valueOf(plan.getTableName()); // 获取表名
      System.out.println("write tableName e:" + tableName);

      ReentrantReadWriteLock.WriteLock writeLock = getWriteLock(tableName);
      System.out.println("write writeLock e:" + writeLock);

      writeLock.lock(); // 加写锁
      planList.add(plan);
      checkTransaction = true;
      return new TransactionFlag(true, "Success");
    } catch (Exception e) {
      System.out.println("writeT e:" + e);
      return new TransactionFlag(false, e.getMessage());
    } finally {
      transactionLock.writeLock().unlock();
    }
  }

  private ReentrantReadWriteLock.ReadLock getReadLock(String tableName) {
    if (!readLocks.containsKey(tableName)) {
      System.out.println("555---WriteLock:" + Manager.getInstance().getCurDB().getTable(tableName));

      //      List<Column> cList = columns;
      //      Column column0 = new Column("column0", ColumnType.INT, 1, false, 0);
      //      Column column1 = new Column("column1", ColumnType.LONG, 0, false, 0);
      //      Column column2 = new Column("column2", ColumnType.FLOAT, 0, false, 0);
      //      Column column3 = new Column("column3", ColumnType.DOUBLE, 0, false, 0);
      //      Column column4 = new Column("column4", ColumnType.STRING, 0, false, 5);
      //      columns.add(column0);
      //      columns.add(column1);
      //      columns.add(column2);
      //      columns.add(column3);
      //      columns.add(column4);
      //      if(Manager.getInstance().getCurDB().getTable(tableName)==null)
      //      {
      //        System.out.println("TABLE NULL");
      //        Manager.getInstance().getCurDB().create(tableName,cList.toArray(new
      // Column[cList.size()]));
      //      }

      Table table = manager.getCurDB().getTable(tableName);
      System.out.println("1---WriteLock:" + table);
      System.out.println("1---WriteLock:" + table.getWriteLock());

      ReentrantReadWriteLock.ReadLock lock = table.getReadLock();
      System.out.println("2---WriteLock:" + tableName);

      readLocks.put(tableName, lock);
    }
    return readLocks.get(tableName);
  }

  private ReentrantReadWriteLock.WriteLock getWriteLock(String tableName) {
    if (!writeLocks.containsKey(tableName)) {
      //      List<Column> cList = columns;
      //      Column column0 = new Column("column0", ColumnType.INT, 1, false, 0);
      //      Column column1 = new Column("column1", ColumnType.LONG, 0, false, 0);
      //      Column column2 = new Column("column2", ColumnType.FLOAT, 0, false, 0);
      //      Column column3 = new Column("column3", ColumnType.DOUBLE, 0, false, 0);
      //      Column column4 = new Column("column4", ColumnType.STRING, 0, false, 5);
      //      columns.add(column0);
      //      columns.add(column1);
      //      columns.add(column2);
      //      columns.add(column3);
      //      columns.add(column4);
      //      if(Manager.getInstance().getCurDB().getTable(tableName)==null)
      //      {
      //        Manager.getInstance().getCurDB().create(tableName,cList.toArray(new
      // Column[cList.size()]));
      //      }

      Table table = manager.getCurDB().getTable(tableName);
      System.out.println("1---WriteLock:" + tableName);

      ReentrantReadWriteLock.WriteLock lock = table.getWriteLock();
      System.out.println("2---WriteLock:" + tableName);

      writeLocks.put(tableName, lock);
    }
    return writeLocks.get(tableName);
  }

  private void releaseAllLocks() {
    for (ReentrantReadWriteLock.ReadLock readLock : readLocks.values()) {
      readLock.unlock();
    }
    for (ReentrantReadWriteLock.WriteLock writeLock : writeLocks.values()) {
      writeLock.unlock();
    }
    readLocks.clear();
    writeLocks.clear();
  }
}
