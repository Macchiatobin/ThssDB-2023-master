package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.rpc.thrift.ExecuteStatementResp;
import cn.edu.thssdb.schema.Database;
import cn.edu.thssdb.schema.Manager;
import cn.edu.thssdb.utils.StatusUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

public class DropTablePlan extends LogicalPlan {

  private String tableName;

  public DropTablePlan(String tableName) {
    super(LogicalPlanType.DROP_TABLE);
    this.tableName = tableName;
  }

  @Override
  public ArrayList<String> getTableName() {
    return new ArrayList<>(Collections.singletonList(this.tableName));
  }

  @Override
  public LinkedList<String> getLog() {
    return super.getLog();
  }

  //  @Override
  //  public ExecuteStatementResp execute_plan(long the_session) {
  //    return null;
  //  }

  @Override
  public ExecuteStatementResp execute_plan() {

    Manager manager = Manager.getInstance();
    Database dbForTableDrop = manager.getCurDB();
    if (dbForTableDrop == null) {
      return new ExecuteStatementResp(StatusUtil.fail("Use database first."), false);
    }
    try {
      dbForTableDrop.drop(tableName);
    } catch (Exception e) {
      return new ExecuteStatementResp(StatusUtil.fail(e.toString()), false);
    }
    return new ExecuteStatementResp(StatusUtil.success(), false);
  }

  @Override
  public String toString() {
    return "DropTablePlan{" + "tableName='" + tableName + '\'' + '}';
  }
}
