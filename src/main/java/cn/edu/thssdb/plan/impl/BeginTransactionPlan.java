package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.rpc.thrift.ExecuteStatementResp;

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
    return null;
  }
}
