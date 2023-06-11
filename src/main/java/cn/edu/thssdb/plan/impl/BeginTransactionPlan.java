package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.rpc.thrift.ExecuteStatementResp;
import cn.edu.thssdb.schema.Database;
import cn.edu.thssdb.schema.Manager;
import cn.edu.thssdb.transaction.MainTransaction;

public class BeginTransactionPlan extends LogicalPlan {

  public BeginTransactionPlan() {
    super(LogicalPlanType.BEGIN_TRANSACTION);
  }

  @Override
  public String toString() {
    return "BeginTransactionPlan";
  }

  @Override
  public ExecuteStatementResp execute_plan() {
    // TODO
    Manager manager = Manager.getInstance();
    Database database = manager.getCurDB();
    String databaseName = database.getdatabaseName();
    MainTransaction mainTransaction = new MainTransaction(databaseName);
    return null;
  }
}
