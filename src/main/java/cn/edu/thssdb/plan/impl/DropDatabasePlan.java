package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.rpc.thrift.ExecuteStatementResp;
import cn.edu.thssdb.schema.Manager;
import cn.edu.thssdb.utils.StatusUtil;

public class DropDatabasePlan extends LogicalPlan {

  private String databaseName;

  public DropDatabasePlan(String databaseName) {
    super(LogicalPlanType.DROP_DB);
    this.databaseName = databaseName;
  }

  public String getDatabaseName() {
    return databaseName;
  }

  @Override
  public ExecuteStatementResp execute_plan() {
    Manager manager = Manager.getInstance();
    try {
      manager.deleteDatabase(databaseName);
    } catch (Exception e) {
      return new ExecuteStatementResp(StatusUtil.fail(e.toString()), false);
    }
    return new ExecuteStatementResp(StatusUtil.success(), false);
  }

  @Override
  public String toString() {
    return "DropDatabasePlan{" + "databaseName='" + databaseName + '\'' + '}';
  }
}
