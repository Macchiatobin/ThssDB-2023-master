package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.rpc.thrift.ExecuteStatementResp;

import java.util.ArrayList;
import java.util.Collections;

public class ShowTablePlan extends LogicalPlan {

  private String tableName;

  public ShowTablePlan(String tableName) {
    super(LogicalPlanType.SHOW_TABLE);
    this.tableName = tableName;
  }

  public ArrayList<String> getTableName() {
    return new ArrayList<>(Collections.singletonList(this.tableName));
  }

  public String getSpecificTableName() {
    return tableName;
  }

  @Override
  public String toString() {
    return "ShowTablePlan{" + "tableName='" + tableName + '\'' + '}';
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
