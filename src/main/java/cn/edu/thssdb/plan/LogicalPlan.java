package cn.edu.thssdb.plan;

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
}
