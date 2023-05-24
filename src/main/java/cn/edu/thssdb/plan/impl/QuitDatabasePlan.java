package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;

public class QuitDatabasePlan extends LogicalPlan {

  // private String databaseName;

  public QuitDatabasePlan() {
    super(LogicalPlanType.QUIT_DB);
    // this.databaseName = databaseName;
  }

  /*
  public String getDatabaseName() {
      return databaseName;
  }
   */

  @Override
  public String toString() {
    return "QuitDatabasePlan";
  }
}
