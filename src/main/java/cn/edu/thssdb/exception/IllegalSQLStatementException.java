package cn.edu.thssdb.exception;

/** 描述：处理属性名称冲突的情况，比如name同时在a，b里有还没指明白a.name还是b.name 参数：属性名称 */
public class IllegalSQLStatementException extends RuntimeException {

  String error_message;

  public IllegalSQLStatementException(String message) {
    this.error_message = message;
  }

  @Override
  public String getMessage() {
    return "Exception: illegal SQL statement! Error: " + error_message;
  }
}
