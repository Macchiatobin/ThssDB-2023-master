package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.parser.MySQLParser;
import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.query.QueryResult;
import cn.edu.thssdb.rpc.thrift.ExecuteStatementResp;
import cn.edu.thssdb.schema.Database;
import cn.edu.thssdb.schema.Manager;
import cn.edu.thssdb.schema.Table;
import cn.edu.thssdb.utils.StatusUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class CommitPlan extends LogicalPlan {
  public static MySQLParser handler;
  ArrayList<QueryResult> the_result;
  ArrayList<QueryResult> result = new ArrayList<>();
  private Manager manager;
  private Database database;

  public CommitPlan(Manager manager, Database database) {
    super(LogicalPlanType.COMMIT);
    this.manager = manager;
    this.database = database;
    handler = new MySQLParser(manager);
  }

  @Override
  public String toString() {
    return "CommitPlan";
  }

  @Override
  public ExecuteStatementResp execute_plan() {
    return null;
  }

  @Override
  public ExecuteStatementResp execute_plan(long the_session) {
    // TODO
    long session = 0;
    try {
      if (manager.transaction_sessions.contains(session)) {
        String db_name = database.getName();
        manager.transaction_sessions.remove(session);
        ArrayList<String> table_list = manager.writeLockMap.get(session);
        for (String table_name : table_list) {
          Table the_table = database.getTable(table_name);
          the_table.releaseWriteLock(session);
        }
        table_list.clear();
        manager.writeLockMap.put(session, table_list);

        String log_name = "data/" + db_name + ".log";
        File file = new File(log_name);
        if (file.exists() && file.isFile() && file.length() > 50000) {
          System.out.println("Clear database Log");
          try {
            FileWriter writer = new FileWriter(log_name);
            writer.write("");
            writer.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
          manager.persist();
        }
        the_result = handler.evaluate("Commit", the_session);
        result.addAll(the_result);
      } else {
        System.out.println("Not in transaction");
      }
      // System.out.println("sessions: "+manager.transaction_sessions);
    } catch (Exception e) {
      return new ExecuteStatementResp(StatusUtil.fail(e.getMessage()), false);
    }
    return new ExecuteStatementResp(StatusUtil.success(), false);
  }
}
