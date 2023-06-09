package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.rpc.thrift.ExecuteStatementResp;
import cn.edu.thssdb.schema.Column;
import cn.edu.thssdb.schema.Database;
import cn.edu.thssdb.schema.Manager;
import cn.edu.thssdb.utils.StatusUtil;

import java.util.*;

public class CreateTablePlan extends LogicalPlan {

  private String tableName;
  private List<Column> columns;
  private String stmt;

  public CreateTablePlan(String tableName, List<Column> columnList, String stmt) {
    super(LogicalPlanType.CREATE_TABLE);
    this.tableName = tableName;
    this.columns = columnList;
    this.stmt = stmt;
  }

  @Override
  public ArrayList<String> getTableName() {
    return new ArrayList<>(Collections.singletonList(this.tableName));
  }

  @Override
  public LinkedList<String> getLog() {
    return new LinkedList<>(Arrays.asList(stmt));
  }

  public List<Column> getColumns() {
    return columns;
  }

  //  @Override
  //  public ExecuteStatementResp execute_plan(long the_session) {
  //    return null;
  //  }

  @Override
  public ExecuteStatementResp execute_plan() {
    Manager manager = Manager.getInstance();
    Database dbForTableCreate = manager.getCurDB();
    if (dbForTableCreate == null) {
      return new ExecuteStatementResp(StatusUtil.fail("Use database first."), false);
    }
    try {
      List<Column> cList = columns;
      dbForTableCreate.create(tableName, cList.toArray(new Column[cList.size()]));
      System.out.println("555---:" + Arrays.toString(cList.toArray(new Column[0])));
    } catch (Exception e) {
      return new ExecuteStatementResp(StatusUtil.fail(e.toString()), false);
    }
    return new ExecuteStatementResp(StatusUtil.success(), false);
  }

  @Override
  public String toString() {
    return "CreateTablePlan{" + "tableName='" + tableName + '\'' + "; columns=" + columns + '}';
  }
}
