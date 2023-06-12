package cn.edu.thssdb.exception;

public class NullComparisonException extends RuntimeException {

  public NullComparisonException() {}

  @Override
  public String getMessage() {
    return "Exception: you are trying to compare a null value!";
  }
}
