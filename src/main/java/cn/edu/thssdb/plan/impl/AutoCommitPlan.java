package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;

public class AutoCommitPlan extends LogicalPlan {

  public AutoCommitPlan() {
    super(LogicalPlanType.AUTO_COMMIT);
  }

  @Override
  public String toString() {
    return "AutoCommitPlan";
  }
}
