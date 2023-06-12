/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package cn.edu.thssdb.parser;

import static cn.edu.thssdb.type.ColumnType.*;

import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.plan.impl.*;
import cn.edu.thssdb.schema.Column;
import cn.edu.thssdb.sql.SQLBaseVisitor;
import cn.edu.thssdb.sql.SQLParser;
import cn.edu.thssdb.type.ColumnType;
import java.util.ArrayList;
import java.util.List;

public class ThssDBSQLVisitor extends SQLBaseVisitor<LogicalPlan> {

  @Override
  public LogicalPlan visitParse(SQLParser.ParseContext ctx) {
    return visit(ctx.getChild(0));
  }

  @Override
  public LogicalPlan visitCreateDbStmt(SQLParser.CreateDbStmtContext ctx) {
    return new CreateDatabasePlan(ctx.databaseName().getText());
  }

  // 以下方法都需要返回相应的、新定义的类实例

  @Override
  public LogicalPlan visitDropDbStmt(SQLParser.DropDbStmtContext ctx) {
    // TODO
    return new DropDatabasePlan(ctx.databaseName().getText());
  }

  @Override
  public LogicalPlan visitUseDbStmt(SQLParser.UseDbStmtContext ctx) {
    return new UseDatabasePlan(ctx.databaseName().getText());
  }

  @Override
  public LogicalPlan visitCreateTableStmt(SQLParser.CreateTableStmtContext ctx) {
    String tableName = ctx.tableName().getText();
    List<Column> columns = new ArrayList<>();
    List<String> pkName = new ArrayList<>();
    for (SQLParser.ColumnDefContext element : ctx.columnDef()) {
      String cName = element.columnName().getText(); // column name
      String cType = element.typeName().getText().toUpperCase(); // column type
      ColumnType type = INT;
      int maxLength = 0;
      if (cType.length() >= 6 && cType.charAt(0) == 'S') // String
      {
        String numString = cType.substring(7, cType.length() - 1);
        maxLength = Integer.parseInt(numString);
        type = STRING;
      } else {
        if (cType.length() == 3) type = INT;
        else if (cType.length() == 4) type = LONG;
        else if (cType.length() == 5) type = FLOAT;
        else type = DOUBLE;
      }
      int pk = 0;
      boolean nn = false;
      for (SQLParser.ColumnConstraintContext cc : element.columnConstraint()) {
        // notnull or primary key
        char t = cc.getText().charAt(0);
        if (t == 'n') nn = true; // notnull
        else pk = 1; // primary key
      }
      Column curColumn = new Column(cName, type, pk, nn, maxLength);
      columns.add(curColumn);
    }
    if (ctx.tableConstraint() != null) {
      for (SQLParser.ColumnNameContext cn : ctx.tableConstraint().columnName()) {
        String keyColumnName = cn.getText(); // primary key column names
        for (Column c : columns) {
          if (c.getName().equals(keyColumnName)) {
            c.setPrimary(1);
          }
        }
      }
    }
    return new CreateTablePlan(tableName, columns);
  }

  @Override
  public LogicalPlan visitDropTableStmt(SQLParser.DropTableStmtContext ctx) {
    return new DropTablePlan(ctx.tableName().getText());
  }

  @Override
  public LogicalPlan visitShowTableStmt(SQLParser.ShowTableStmtContext ctx) {
    return new ShowTablePlan(ctx.tableName().getText());
  }

  @Override
  public LogicalPlan visitAutoCommitStmt(SQLParser.AutoCommitStmtContext ctx) {
    return new AutoCommitPlan();
  }

  public LogicalPlan visitBeginTransactionStmt(SQLParser.BeginTransactionStmtContext ctx) {
    return new BeginTransactionPlan();
  }

  public LogicalPlan visitCommitStmt(SQLParser.CommitStmtContext ctx) {
    return new CommitPlan();
  }

  @Override
  public LogicalPlan visitInsertStmt(SQLParser.InsertStmtContext ctx) {
    String tableName = ctx.tableName().getText();
    List<String> columnName = new ArrayList<>();
    List<String> valueEntry = new ArrayList<>();

    for (SQLParser.ColumnNameContext e : ctx.columnName()) {
      String cur_columnName = e.IDENTIFIER().getText();
      columnName.add(cur_columnName);
    }

    for (SQLParser.ValueEntryContext e : ctx.valueEntry()) {
      List<SQLParser.LiteralValueContext> lctx = e.literalValue();
      for (SQLParser.LiteralValueContext lc : lctx) {
        String cur_value = "";
        if (lc.NUMERIC_LITERAL() != null) {
          cur_value = lc.NUMERIC_LITERAL().getText();
          // TODO: parseInt? parseDouble? parseFloat?
        } else if (lc.STRING_LITERAL() != null) {
          cur_value = lc.STRING_LITERAL().getText();
        } else if (lc.K_NULL() != null) {
          cur_value = null;
        }
        valueEntry.add(cur_value);
      }
    }

    return new InsertPlan(tableName, columnName, valueEntry);
  }

  @Override
  public LogicalPlan visitDeleteStmt(SQLParser.DeleteStmtContext ctx) { // only one expression
    String tableName = ctx.tableName().getText();
    SQLParser.ConditionContext condition = ctx.multipleCondition().condition();
    String attrname = condition.expression(0).getText();
    String attrvalue = condition.expression(1).getText();
    String comparator = condition.comparator().getText();

    return new DeletePlan(tableName, attrname, attrvalue, comparator);
  }

  @Override
  public LogicalPlan visitUpdateStmt(SQLParser.UpdateStmtContext ctx) { // only one where expression, set always '='
    String tableName = ctx.tableName().getText();
    String set_column_name = ctx.columnName().getText();
    String set_attr_value = ctx.expression().getText();
    SQLParser.ConditionContext condition = ctx.multipleCondition().condition();
    String where_attr_name = condition.expression(0).getText();
    String where_attr_value = condition.expression(1).getText();
    String comparator = condition.comparator().getText();

    return new UpdatePlan(tableName, set_column_name, set_attr_value,
            where_attr_name, where_attr_value, comparator);
  }

  @Override
  public LogicalPlan visitQuitStmt(SQLParser.QuitStmtContext ctx) {
    return new QuitPlan();
  }

  // TODO: parser to more logical plan
}
