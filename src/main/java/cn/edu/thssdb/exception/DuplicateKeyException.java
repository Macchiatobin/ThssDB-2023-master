package cn.edu.thssdb.exception;

public class DuplicateKeyException extends RuntimeException {
  @Override
  public String getMessage() {
    return "Exception: insertion caused duplicated keys!";
  }

  @Override
  public String toString() {
    return "Duplicate Key!";
  }
}
