package cn.edu.thssdb.transaction;

import cn.edu.thssdb.exception.NotExistsException;
import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.schema.Manager;
import cn.edu.thssdb.schema.Table;
import cn.edu.thssdb.utils.Global;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MainTransaction {
  private Manager manager;
  private String databaseName;
  private LinkedList<LogicalPlan> planList;
  private LinkedList<ReentrantReadWriteLock.ReadLock> readLockLinkedList;
  private LinkedList<ReentrantReadWriteLock.WriteLock> writeLockLinkedList;
  private boolean checkTransaction = false;

  public MainTransaction(String databaseName) {
    this.manager = Manager.getInstance();
    this.databaseName = databaseName;
    this.planList = new LinkedList<>();
    this.readLockLinkedList = new LinkedList<>();
    this.writeLockLinkedList = new LinkedList<>();
  }

  public TransactionFlag exec(LogicalPlan plan) {
    switch (plan.getType()) {
      case BEGIN_TRANSACTION:
        return beginTransaction();
      case COMMIT:
        return commitTransaction();
      case SELECT:
        return readTransaction(plan);
      case INSERT:
      case UPDATE:
      case DELETE:
        return writeTransaction(plan);
      default:
        return null;
    }
  }

  private TransactionFlag beginTransaction() {
    if (databaseName == null) {
      throw new NotExistsException(1, "");
    }
    if (checkTransaction) {
      return new TransactionFlag(false, "Other plan is working");
    } else {
      checkTransaction = true;
      return new TransactionFlag(true, "Success");
    }
  }

  private TransactionFlag commitTransaction() {
    if (databaseName == null) {
      throw new NotExistsException(1, "");
    }
    this.releaseTransactionReadWriteLock();
    while (!planList.isEmpty()) {
      LogicalPlan lp = planList.getFirst();
      planList.removeFirst();
    }
    checkTransaction = false;
    return new TransactionFlag(true, "Success");
  }

  private TransactionFlag readTransaction(LogicalPlan plan) {
    if (Global.DATABASE_ISOLATION_LEVEL == Global.ISOLATION_LEVEL.READ_UNCOMMITTED) {
      try {
        plan.execute_plan();
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
        plan.execute_plan();
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
        plan.execute_plan();
        checkTransaction = true;
      } catch (Exception e) {
        return new TransactionFlag(false, e.getMessage());
      }
      return new TransactionFlag(true, "Success");
    }
    return new TransactionFlag(false, "Failure cause ISOLATION_LEVEL error");
  }

  private TransactionFlag writeTransaction(LogicalPlan plan) {
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
        plan.execute_plan();
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
