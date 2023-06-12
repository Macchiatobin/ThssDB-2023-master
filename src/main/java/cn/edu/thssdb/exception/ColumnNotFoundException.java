package cn.edu.thssdb.exception;

public class ColumnNotFoundException extends RuntimeException {
  private String col_name;

  public ColumnNotFoundException(String name) {
    col_name = name;
  }

  @Override
  public String getMessage() {
    return "Exception: Column \'" + col_name + "\' does not exist!";
  }
}
