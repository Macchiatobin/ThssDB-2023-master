package cn.edu.thssdb.transaction;

import cn.edu.thssdb.exception.NotExistsException;
import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.plan.impl.*;
import cn.edu.thssdb.schema.Logger;
import cn.edu.thssdb.schema.Manager;
import cn.edu.thssdb.schema.Table;
import cn.edu.thssdb.utils.Global;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MainTransaction {
  private Manager manager;
  private String databaseName;
  private Logger logger; // 日志对象

  private LinkedList<LogicalPlan> planList;
  private LinkedList<ReentrantReadWriteLock.ReadLock> readLockLinkedList;
  private LinkedList<ReentrantReadWriteLock.WriteLock> writeLockLinkedList;
  private boolean checkTransaction = false;

  public MainTransaction(String databaseName,Logger logger) {
    this.manager = Manager.getInstance();
    this.databaseName = databaseName;
    this.logger = logger;
    this.planList = new LinkedList<>();
    this.readLockLinkedList = new LinkedList<>();
    this.writeLockLinkedList = new LinkedList<>();
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

  private TransactionFlag endTransaction(LogicalPlan plan) {
    if (checkTransaction) {
      commitTransaction();
    }
    try {
      //      plan.execute_plan();
      checkTransaction = false;
    } catch (Exception e) {
      return new TransactionFlag(false, e.getMessage());
    }
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
      log.add("Begin Transaction");
      logger.writeLines(log);
      return new TransactionFlag(true, "Success");
    }
  }

  private TransactionFlag commitTransaction() {
    System.out.println("commitT check:" + checkTransaction);

    if (databaseName == null) {
      throw new NotExistsException(1, "");
    }
    this.releaseTransactionReadWriteLock();
    LinkedList<String> log = new LinkedList<>();
    while (!planList.isEmpty()) {
      LogicalPlan lp = planList.getFirst();
      log.addAll(lp.getLog());
      planList.removeFirst();
    }
    log.add("Commit");
    logger.writeLines(log);
    checkTransaction = false;
    return new TransactionFlag(true, "Success");
  }

  private TransactionFlag readTransaction(LogicalPlan plan) {
    System.out.println("readT check:" + checkTransaction);

    if (Global.DATABASE_ISOLATION_LEVEL == Global.ISOLATION_LEVEL.READ_UNCOMMITTED) {
      try {
        //        plan.execute_plan();
        checkTransaction = true;
      } catch (Exception e) {
        return new TransactionFlag(false, e.getMessage());
      }
      return new TransactionFlag(true, "Success");
    }

    if (Global.DATABASE_ISOLATION_LEVEL == Global.ISOLATION_LEVEL.READ_COMMITTED) {
      ArrayList<String> tableName = plan.getTableName();
      if (tableName != null) {
        for (String tmp_tableName : tableName) {
          this.getTransactionReadLock(tmp_tableName);
        }
      }

      try {
        //        plan.execute_plan();
        checkTransaction = true;
      } catch (Exception e) {
        return new TransactionFlag(false, e.getMessage());
      }

      if (tableName != null) {
        for (String tmp_tableName : tableName) {
          this.releaseTransactionReadLock(tmp_tableName);
        }
      }
      return new TransactionFlag(true, "Success");
    }

    if (Global.DATABASE_ISOLATION_LEVEL == Global.ISOLATION_LEVEL.SERIALIZABLE) {
      ArrayList<String> tableName = plan.getTableName();
      if (tableName != null) {
        for (String tmp_tableName : tableName) {
          this.getTransactionReadLock(tmp_tableName);
        }
      }

      try {
        //        plan.execute_plan();
        checkTransaction = true;
      } catch (Exception e) {
        return new TransactionFlag(false, e.getMessage());
      }
      return new TransactionFlag(true, "Success");
    }
    return new TransactionFlag(false, "Failure cause ISOLATION_LEVEL error");
  }

  private TransactionFlag writeTransaction(LogicalPlan plan) {
    System.out.println("writeT check:" + checkTransaction);

    if ((Global.DATABASE_ISOLATION_LEVEL == Global.ISOLATION_LEVEL.READ_COMMITTED)
        || (Global.DATABASE_ISOLATION_LEVEL == Global.ISOLATION_LEVEL.READ_UNCOMMITTED)
        || (Global.DATABASE_ISOLATION_LEVEL == Global.ISOLATION_LEVEL.SERIALIZABLE)) {
      ArrayList<String> tableName = plan.getTableName();
      if (tableName != null) {
        for (String tmp_tableName : tableName) {
          this.getTransactionWriteLock(tmp_tableName);
        }
      }

      try {
        System.out.println("Write Trans plan execute!");
        //          plan.execute_plan();
        System.out.println("Plan: " + plan);
        planList.add(plan);
        checkTransaction = true;
      } catch (Exception e) {
        return new TransactionFlag(false, e.getMessage());
      }
      return new TransactionFlag(true, "Success");
    }

    return new TransactionFlag(false, "Failure cause ISOLATION_LEVEL error");
  }

  private void getTransactionReadLock(String tableName) {
    if (!Global.ISOLATION_STATUS) {
      return;
    }
    Table table = manager.getCurDB().getTable(tableName);
    if (table == null) {
      return;
    }
    ReentrantReadWriteLock.ReadLock readLock = table.lock.readLock();
    readLock.lock();
    readLockLinkedList.add(readLock);
  }

  private void getTransactionWriteLock(String tableName) {
    if (!Global.ISOLATION_STATUS) {
      return;
    }
    Table table = manager.getCurDB().getTable(tableName);
    if (table == null) {
      return;
    }
    ReentrantReadWriteLock.WriteLock writeLock = table.lock.writeLock();
    if (!writeLock.tryLock()) {
      while (true) {
        if (!this.releaseTransactionReadLock(tableName)) {
          break;
        }
      }
      writeLock.lock();
    }
    writeLockLinkedList.add(writeLock);
  }

  private boolean releaseTransactionReadLock(String tableName) {
    if (!Global.ISOLATION_STATUS) {
      return true;
    }
    Table table = manager.getCurDB().getTable(tableName);
    if (table == null) {
      return false;
    }
    ReentrantReadWriteLock.ReadLock readLock = table.lock.readLock();
    if (readLockLinkedList.remove(readLock)) {
      readLock.unlock();
      return true;
    }
    return false;
  }

  private void releaseTransactionReadWriteLock() {
    if (!Global.ISOLATION_STATUS) {
      return;
    }
    while (!writeLockLinkedList.isEmpty()) {
      writeLockLinkedList.remove().unlock();
    }
    while (!readLockLinkedList.isEmpty()) {
      readLockLinkedList.remove().unlock();
    }
  }
}
