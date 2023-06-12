package cn.edu.thssdb.exception;

/** 描述：处理属性名称异常的情况，比如a.b.kebab 参数：属性名称 */
public class InvalidColumnNameException extends RuntimeException {
  private String mName;

  public InvalidColumnNameException(String name) {
    mName = name;
  }

  @Override
  public String getMessage() {
    return "Exception: Column Name \'" + mName + "\' is invalid!";
  }
}
