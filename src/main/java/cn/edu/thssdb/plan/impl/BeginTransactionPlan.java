package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.parser.MySQLParser;
import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.query.QueryResult;
import cn.edu.thssdb.rpc.thrift.ExecuteStatementResp;
import cn.edu.thssdb.schema.Manager;
import cn.edu.thssdb.utils.StatusUtil;

import java.util.ArrayList;

public class BeginTransactionPlan extends LogicalPlan {
  public static MySQLParser handler;
  ArrayList<QueryResult> the_result;
  ArrayList<QueryResult> result = new ArrayList<>();
  private Manager manager;

  public BeginTransactionPlan() {
    super(LogicalPlanType.BEGIN_TRANSACTION);
  }

  @Override
  public String toString() {
    return "BeginTransactionPlan";
  }

  @Override
  public ExecuteStatementResp execute_plan() {
    System.out.println("BEGIN PLAN NULL");
    return new ExecuteStatementResp(StatusUtil.success(), false);
  }
}
