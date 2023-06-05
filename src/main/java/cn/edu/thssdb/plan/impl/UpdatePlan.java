package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.rpc.thrift.ExecuteStatementResp;

public class UpdatePlan extends LogicalPlan {

  public UpdatePlan() {
    super(LogicalPlanType.UPDATE);
  }

  @Override
  public String toString() {
    return "UpdatePlan";
  }

  @Override
  public ExecuteStatementResp execute_plan() {
    // TODO
    return null;
  }
}
