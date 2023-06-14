package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.rpc.thrift.ExecuteStatementResp;
import cn.edu.thssdb.schema.Database;
import cn.edu.thssdb.schema.Manager;
import cn.edu.thssdb.schema.Table;
import cn.edu.thssdb.utils.StatusUtil;
import java.util.ArrayList;

public class AutoCommitPlan extends LogicalPlan {
  private Manager manager;
  private Database database;

  public AutoCommitPlan(Manager manager, Database database) {
    super(LogicalPlanType.AUTO_COMMIT);
    this.manager = manager;
    this.database = database;
  }

  @Override
  public String toString() {
    return "AutoCommitPlan";
  }

  @Override
  public ExecuteStatementResp execute_plan(long the_session) {
    return null;
  }

  @Override
  public ExecuteStatementResp execute_plan() {
    // TODO
    long session = 0;
    try {
      if (manager.transaction_sessions.contains(session)) {
        manager.transaction_sessions.remove(session);
        ArrayList<String> table_list = manager.writeLockMap.get(session);
        for (String table_name : table_list) {
          Table the_table = database.getTable(table_name);
          the_table.releaseWriteLock(session);
        }
        table_list.clear();
        manager.writeLockMap.put(session, table_list);
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
