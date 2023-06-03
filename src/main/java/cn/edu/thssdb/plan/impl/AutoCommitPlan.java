package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.rpc.thrift.ExecuteStatementResp;

public class AutoCommitPlan extends LogicalPlan {

  public AutoCommitPlan() {
    super(LogicalPlanType.AUTO_COMMIT);
  }

  @Override
  public String toString() {
    return "AutoCommitPlan";
  }

  @Override
  public ExecuteStatementResp execute_plan() {
    // TODO
    return null;
  }
}
