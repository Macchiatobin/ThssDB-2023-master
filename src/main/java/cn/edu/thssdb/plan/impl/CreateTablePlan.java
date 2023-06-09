package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.rpc.thrift.ExecuteStatementResp;
import cn.edu.thssdb.schema.Column;
import cn.edu.thssdb.schema.Database;
import cn.edu.thssdb.schema.Manager;
import cn.edu.thssdb.utils.StatusUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CreateTablePlan extends LogicalPlan {

  private String tableName;
  private List<Column> columns;

  public CreateTablePlan(String tableName, List<Column> columnList) {
    super(LogicalPlanType.CREATE_TABLE);
    this.tableName = tableName;
    this.columns = columnList;
  }

  public ArrayList<String> getTableName() {
    return new ArrayList<>(Collections.singletonList(this.tableName));
  }

  public List<Column> getColumns() {
    return columns;
  }

  @Override
  public ExecuteStatementResp execute_plan() {
    Manager manager = Manager.getInstance();
    Database dbForTableCreate = manager.getCurDB();
    if (dbForTableCreate == null) {
      return new ExecuteStatementResp(StatusUtil.fail("Use database first."), false);
    }
    List<Column> cList = columns;
    dbForTableCreate.create(tableName, cList.toArray(new Column[cList.size()]));
    return new ExecuteStatementResp(StatusUtil.success(), false);
  }

  @Override
  public String toString() {
    return "CreateTablePlan{" + "tableName='" + tableName + '\'' + "; columns=" + columns + '}';
  }
}
