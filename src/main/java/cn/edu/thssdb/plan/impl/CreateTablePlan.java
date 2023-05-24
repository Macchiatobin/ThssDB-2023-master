package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.sql.SQLParser;

import java.util.List;

public class CreateTablePlan extends LogicalPlan {

  private String tableName;
  private String columnDefContext;
  private List<SQLParser.ColumnDefContext> columnDefContextList;
  private String tableConstraintContext;
  // TODO: check if types are compatible

  public CreateTablePlan(
      String tableName,
      String columnDefContext,
      List<SQLParser.ColumnDefContext> columnDefContextList,
      String tableConstraintContext) {
    super(LogicalPlanType.CREATE_TABLE);

    this.tableName = tableName;
    this.columnDefContext = columnDefContext;
    this.columnDefContextList = columnDefContextList;
    this.tableConstraintContext = tableConstraintContext;
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
