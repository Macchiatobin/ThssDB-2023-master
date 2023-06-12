package cn.edu.thssdb.exception;

import cn.edu.thssdb.type.ComparerType;

/** 描述：待比较参数类型不匹配异常 参数：两个类型 */
public class MisMatchTypeException extends RuntimeException {

  private ComparerType left_type, right_type;

  public MisMatchTypeException(ComparerType left, ComparerType right) {
    left_type = left;
    right_type = right;
  }

  @Override
  public String getMessage() {
    String type1, type2;
    if (left_type == ComparerType.NUMBER) type1 = "NUMBER";
    else if (left_type == ComparerType.COLUMN) type1 = "COLUMN";
    else if (left_type == ComparerType.STRING) type1 = "STRING";
    else type1 = "NULL";
    if (right_type == ComparerType.NUMBER) type2 = "NUMBER";
    else if (right_type == ComparerType.COLUMN) type2 = "COLUMN";
    else if (right_type == ComparerType.STRING) type2 = "STRING";
    else type2 = "NULL";
    return "Exception: left type \'"
        + type1
        + "\' and right type \'"
        + type2
        + "\' cannot be compared!";
  }
}
