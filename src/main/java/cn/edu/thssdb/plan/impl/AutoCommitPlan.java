package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.rpc.thrift.ExecuteStatementResp;
import cn.edu.thssdb.schema.Database;
import cn.edu.thssdb.schema.Manager;
import cn.edu.thssdb.utils.StatusUtil;

public class AutoCommitPlan extends LogicalPlan {
  private Manager manager;
  private Database database;

  public AutoCommitPlan() {
    super(LogicalPlanType.AUTO_COMMIT);
  }

  @Override
  public String toString() {
    return "AutoCommitPlan";
  }

  @Override
  public ExecuteStatementResp execute_plan() {
    return new ExecuteStatementResp(StatusUtil.success(), false);
  }
}
