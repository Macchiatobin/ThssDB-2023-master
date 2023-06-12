package cn.edu.thssdb.exception;

public class QueryResultException extends RuntimeException {

  public QueryResultException() {}

  @Override
  public String getMessage() {
    return "Exception: Query Result Error!";
  }
}
