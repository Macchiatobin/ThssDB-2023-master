package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.schema.Column;

import java.util.List;

public class CreateTablePlan extends LogicalPlan {

  private String tableName;
  private List<Column> columns;

  public CreateTablePlan(String tableName, List<Column> columnList) {
    super(LogicalPlanType.CREATE_TABLE);
    this.tableName = tableName;
    this.columns = columnList;
  }

  public String getTableName() {
    return tableName;
  }

  public List<Column> getColumns() {
    return columns;
  }

  // TODO: define more functions

  @Override
  public String toString() {
    return "CreateTablePlan{" + "tableName='" + tableName + '\'' + '}';
  }
}
