package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.schema.Column;
import cn.edu.thssdb.sql.SQLParser;

import java.util.List;

public class CreateTablePlan extends LogicalPlan {

  private String tableName;
  private List<Column> columns;
  private List<String> keyNames;

  public CreateTablePlan(String tableName, List<Column> columnList, List<String> keyList) {
    super(LogicalPlanType.CREATE_TABLE);
    this.tableName = tableName;
    this.columns = columnList;
    this.keyNames = keyList;
  }

  public String getTableName() {
    return tableName;
  }

  // TODO: define more functions

  @Override
  public String toString() {
    return "CreateTablePlan{" + "tableName='" + tableName + '\'' + '}';
  }
}
