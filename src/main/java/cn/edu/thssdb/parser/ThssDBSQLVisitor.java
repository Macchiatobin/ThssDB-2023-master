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

import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.plan.impl.CreateDatabasePlan;
import cn.edu.thssdb.plan.impl.CreateTablePlan;
import cn.edu.thssdb.plan.impl.DropDatabasePlan;
import cn.edu.thssdb.plan.impl.ShowTablePlan;
import cn.edu.thssdb.sql.SQLBaseVisitor;
import cn.edu.thssdb.sql.SQLParser;

public class ThssDBSQLVisitor extends SQLBaseVisitor<LogicalPlan> {

  @Override
  public LogicalPlan visitCreateDbStmt(SQLParser.CreateDbStmtContext ctx) {
    return new CreateDatabasePlan(ctx.databaseName().getText());
  }

  //TODO 以下方法都需要返回相应的、新定义的类实例

  @Override
  public LogicalPlan visitDropDbStmt(SQLParser.DropDbStmtContext ctx) {
    //TODO
    return new DropDatabasePlan(ctx.databaseName().getText());
  }

  @Override
  public LogicalPlan visitCreateTableStmt(SQLParser.CreateTableStmtContext ctx) {
    //TODO: check ctx structure
    return new CreateTablePlan(ctx.tableName().getText(), ctx.columnDef(1).getText(),
            ctx.columnDef().stream().toList(), ctx.tableConstraint().getText());
  }

  @Override
  public LogicalPlan visitDropTableStmt(SQLParser.DropTableStmtContext ctx) {
    //TODO
    return null;
  }

  @Override
  public LogicalPlan visitShowTableStmt(SQLParser.ShowTableStmtContext ctx) {
    //TODO
    return new ShowTablePlan(ctx.tableName().getText());
  }

  @Override
  public LogicalPlan visitInsertStmt(SQLParser.InsertStmtContext ctx) {
    //TODO
    return null;
  }

  @Override
  public LogicalPlan visitDeleteStmt(SQLParser.DeleteStmtContext ctx) {
    //TODO
    return null;
  }

  @Override
  public LogicalPlan visitUpdateStmt(SQLParser.UpdateStmtContext ctx) {
    //TODO
    return null;
  }
  // TODO: parser to more logical plan
}
