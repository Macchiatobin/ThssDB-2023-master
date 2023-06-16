package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.rpc.thrift.ExecuteStatementResp;

public class QuitPlan extends LogicalPlan {

  public QuitPlan() {
    super(LogicalPlanType.QUIT);
  }

  @Override
  public String toString() {
    return "QuitPlan";
  }

  //  @Override
  //  public ExecuteStatementResp execute_plan(long the_session) {
  //    return null;
  //  }

  @Override
  public ExecuteStatementResp execute_plan() {
    return null;
  }
}
