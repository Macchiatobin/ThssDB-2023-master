package cn.edu.thssdb.query;

import cn.edu.thssdb.type.ComparerType;

/* Expression:
 *      Comparer
 */

public class Expression {
  public ComparerType comparer_type;
  public Comparable comparer_value;

  public Expression(ComparerType type, String value) {
    this.comparer_type = type;
    if (type == ComparerType.NUMBER) this.comparer_value = Double.parseDouble(value);
    else if (type == ComparerType.STRING || type == ComparerType.COLUMN)
      this.comparer_value = value;
    else this.comparer_value = null;
  }

  public String getText() {
    switch (comparer_type) {
      case NUMBER:
        return Double.toString((Double) comparer_value);
      case STRING:
      case COLUMN:
        return (String) comparer_value;
      case NULL:
        return "null";
    }

    return (String) comparer_value;
  }
}
