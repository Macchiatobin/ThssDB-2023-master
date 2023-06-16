package cn.edu.thssdb.plan;

import cn.edu.thssdb.rpc.thrift.ExecuteStatementResp;

import java.util.ArrayList;

public abstract class LogicalPlan {

  protected LogicalPlanType type;

  public LogicalPlan(LogicalPlanType type) {
    this.type = type;
  }

  public LogicalPlanType getType() {
    return type;
  }

  public ArrayList<String> getTableName() {
    return null;
  }

  public enum LogicalPlanType {
    // TODO: add more LogicalPlanType
    CREATE_DB,
    USE_DB,
    DROP_DB,
    QUIT,
    CREATE_TABLE,
    DROP_TABLE,
    AUTO_COMMIT,
    BEGIN_TRANSACTION,
    COMMIT,
    SHOW_TABLE,
    INSERT,
    DELETE,
    UPDATE,
    SELECT,
  }

  public abstract ExecuteStatementResp
      execute_plan(); // Plan execution abstract method, returns response

  //  public abstract ExecuteStatementResp execute_plan(long the_session);
}
