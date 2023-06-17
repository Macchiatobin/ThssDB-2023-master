package cn.edu.thssdb.transaction;

import cn.edu.thssdb.exception.NotExistsException;
import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.plan.impl.*;
import cn.edu.thssdb.schema.Logger;
import cn.edu.thssdb.schema.Manager;
import cn.edu.thssdb.schema.Table;
import cn.edu.thssdb.utils.Global;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MainTransaction {
  private Manager manager;
  private String databaseName;
  private Logger logger; // 日志对象
  private ReentrantReadWriteLock transactionLock;

  private HashMap<String, ReentrantReadWriteLock.ReadLock> readLocks;
  private HashMap<String, ReentrantReadWriteLock.WriteLock> writeLocks;
  private LinkedList<LogicalPlan> planList;
  private boolean checkTransaction = false;

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
      return null;
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
      return null;
    }
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
      Table table = manager.getCurDB().getTable(tableName);
      System.out.println("1---WriteLock:" + tableName);

      ReentrantReadWriteLock lock = table.getLock();
      System.out.println("2---WriteLock:" + tableName);

      readLocks.put(tableName, lock.readLock());
    }
    return readLocks.get(tableName);
  }

  private ReentrantReadWriteLock.WriteLock getWriteLock(String tableName) {
    if (!writeLocks.containsKey(tableName)) {
      Table table = manager.getCurDB().getTable(tableName);
      System.out.println("1---WriteLock:" + tableName);

      ReentrantReadWriteLock lock = table.getLock();
      System.out.println("2---WriteLock:" + tableName);

      writeLocks.put(tableName, lock.writeLock());
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