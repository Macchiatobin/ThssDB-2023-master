package cn.edu.thssdb.plan;

import cn.edu.thssdb.rpc.thrift.ExecuteStatementResp;

public abstract class LogicalPlan {

  protected LogicalPlanType type;

  public LogicalPlan(LogicalPlanType type) {
    this.type = type;
  }

  public LogicalPlanType getType() {
    return type;
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
    UPDATE
  }

  abstract public ExecuteStatementResp execute_plan(); // Plan execution abstract method, returns response
}
