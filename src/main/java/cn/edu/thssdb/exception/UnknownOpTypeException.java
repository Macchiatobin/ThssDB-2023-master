package cn.edu.thssdb.exception;

public class UnknownOpTypeException extends RuntimeException {
  @Override
  public String getMessage() {
    return "Exception: query failed, unknown LogicalOperator type!";
  }
}
