package cn.edu.thssdb.exception;

public class UnknownOperatorException extends RuntimeException {

  public UnknownOperatorException() {}

  @Override
  public String getMessage() {
    return "Exception: unknown logical operator (neither OR or AND)!";
  }
}
