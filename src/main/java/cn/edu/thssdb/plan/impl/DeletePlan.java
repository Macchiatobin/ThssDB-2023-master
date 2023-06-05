package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.rpc.thrift.ExecuteStatementResp;

public class DeletePlan extends LogicalPlan {

  public DeletePlan() {
    super(LogicalPlanType.DELETE);
  }

  @Override
  public String toString() {
    return "DeletePlan";
  }

  @Override
  public ExecuteStatementResp execute_plan() {
    // TODO
    return null;
  }
}
