package cn.edu.thssdb.exception;

/** 描述：select没有选中任何table 参数：无 */
public class NoTableSelectedException extends RuntimeException {
  public NoTableSelectedException() {
    super();
  }

  @Override
  public String getMessage() {
    return "Exception: you did not select a table is for this query!";
  }
}
