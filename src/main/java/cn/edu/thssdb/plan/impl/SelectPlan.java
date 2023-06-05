package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.rpc.thrift.ExecuteStatementResp;

public class SelectPlan extends LogicalPlan {

  public SelectPlan() {
    super(LogicalPlan.LogicalPlanType.SELECT);
  }

  @Override
  public String toString() {
    return "SelectPlan";
  }

  @Override
  public ExecuteStatementResp execute_plan() {
    // TODO
    return null;
  }
}
