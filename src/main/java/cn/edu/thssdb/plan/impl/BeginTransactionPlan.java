package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.rpc.thrift.ExecuteStatementResp;
import cn.edu.thssdb.schema.Manager;
import cn.edu.thssdb.utils.StatusUtil;
import java.util.ArrayList;

public class BeginTransactionPlan extends LogicalPlan {
  private Manager manager;

  public BeginTransactionPlan(Manager manager) {
    super(LogicalPlanType.BEGIN_TRANSACTION);
    this.manager = manager;
  }

  @Override
  public String toString() {
    return "BeginTransactionPlan";
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
      if (!manager.transaction_sessions.contains(session)) {
        manager.transaction_sessions.add(session);
        ArrayList<String> readLockList = new ArrayList<>();
        ArrayList<String> writeLockList = new ArrayList<>();
        manager.readLockMap.put(session, readLockList);
        manager.writeLockMap.put(session, writeLockList);
      } else {
        System.out.println("Begin Transaction");
      }

    } catch (Exception e) {
      return new ExecuteStatementResp(StatusUtil.fail(e.getMessage()), false);
    }
    return new ExecuteStatementResp(StatusUtil.success(), false);
  }
}
