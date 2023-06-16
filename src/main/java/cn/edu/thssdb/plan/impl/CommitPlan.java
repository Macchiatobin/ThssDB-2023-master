package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.parser.MySQLParser;
import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.query.QueryResult;
import cn.edu.thssdb.rpc.thrift.ExecuteStatementResp;
import cn.edu.thssdb.schema.Database;
import cn.edu.thssdb.schema.Manager;
import cn.edu.thssdb.utils.StatusUtil;

import java.util.ArrayList;

public class CommitPlan extends LogicalPlan {
  public static MySQLParser handler;
  ArrayList<QueryResult> the_result;
  ArrayList<QueryResult> result = new ArrayList<>();
  private Manager manager;
  private Database database;

  public CommitPlan() {
    super(LogicalPlanType.COMMIT);
  }

  @Override
  public String toString() {
    return "CommitPlan";
  }

  @Override
  public ExecuteStatementResp execute_plan() {
    return new ExecuteStatementResp(StatusUtil.success(), false);
  }

}
