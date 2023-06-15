package cn.edu.thssdb.query;

import cn.edu.thssdb.exception.MisMatchTypeException;
import cn.edu.thssdb.exception.NullComparisonException;
import cn.edu.thssdb.type.ComparatorType;
import cn.edu.thssdb.type.ComparerType;

/* Condition:
 *      Expression, Comparator(EQ,NE,GT,LT,GE,LE), Expression
 */

public class Condition {
  ComparatorType comparator_type;
  Expression left_exp;
  Expression right_exp;

  public Condition(Expression left, Expression right, ComparatorType type) {
    this.left_exp = left;
    this.right_exp = right;
    this.comparator_type = type;
  }

  public String getText() {
    String comparator_toStr;
    if (comparator_type == ComparatorType.EQ) comparator_toStr = "=";
    else if (comparator_type == ComparatorType.NE) comparator_toStr = "<>";
    else if (comparator_type == ComparatorType.GT) comparator_toStr = ">";
    else if (comparator_type == ComparatorType.LT) comparator_toStr = "<";
    else if (comparator_type == ComparatorType.GE) comparator_toStr = ">=";
    else if (comparator_type == ComparatorType.LE) comparator_toStr = "<=";
    else comparator_toStr = "{UNKNOWN COMPARATOR}";
    return left_exp.getText() + comparator_toStr + right_exp.getText();
  }

  // Obtain query result recursively
  public boolean executeQuery(QueryRow row) {

    // Don't execute queries with null comparisons
    if (left_exp == null || right_exp == null) throw new NullComparisonException();
    else if (left_exp.comparer_type == ComparerType.NULL
        || right_exp.comparer_type == ComparerType.NULL) throw new NullComparisonException();
    else if (left_exp.comparer_value == null || right_exp.comparer_value == null)
      throw new NullComparisonException();
    else {
      //      System.out.println("Condition executeQuery(): no null comparisons"); // debug
      Comparable left_val = left_exp.comparer_value;
      Comparable right_val = right_exp.comparer_value;
      ComparerType left_type = left_exp.comparer_type;
      ComparerType right_type = right_exp.comparer_type;
      //      System.out.println("Condition executeQuery(): vals and types assigned"); // debug
      if (left_type == ComparerType.COLUMN) {
        //        System.out.println("Condition executeQuery(): left_type == ComparerType.COLUMN");
        // // debug
        Expression left_comparer = row.getColumnComparer((String) left_exp.comparer_value);
        //        System.out.println("Condition executeQuery(): getColumnComparer() done"); // debug
        left_val = left_comparer.comparer_value;
        left_type = left_comparer.comparer_type;
      }
      if (right_type == ComparerType.COLUMN) {
        //        System.out.println("Condition executeQuery(): right_type == ComparerType.COLUMN");
        // // debug
        Expression right_comparer = row.getColumnComparer((String) right_exp.comparer_value);
        right_val = right_comparer.comparer_value;
        right_type = right_comparer.comparer_type;
      }

      System.out.println("Condition executeQuery(): left_exp: " + left_exp.getText()); // debug
      System.out.println("Condition executeQuery(): right_exp: " + right_exp.getText()); // debug

      // Don't execute queries with null comparisons
      if (left_type == ComparerType.NULL
          || right_type == ComparerType.NULL
          || left_val == null
          || right_val == null) {
        System.out.println(
            "Condition exeucuteQuery(): comparing with a null type! Throw NullComparisonException."); // debug
        throw new NullComparisonException();
      }

      // Comparison
      if (left_type != right_type) // Different comparer types
      throw new MisMatchTypeException(left_type, right_type);
      else {
        boolean res = false;
        if (comparator_type == ComparatorType.EQ) {
          System.out.println("Condition executeQuery(): comparator_type EQ");
          res = left_val.compareTo(right_val) == 0;
        } else if (comparator_type == ComparatorType.NE) {
          System.out.println("Condition executeQuery(): comparator_type NE");
          res = left_val.compareTo(right_val) != 0;
        } else if (comparator_type == ComparatorType.GT) {
          System.out.println("Condition executeQuery(): comparator_type GT");
          res = left_val.compareTo(right_val) > 0;
        } else if (comparator_type == ComparatorType.LT) {
          System.out.println("Condition executeQuery(): comparator_type LT");
          res = left_val.compareTo(right_val) < 0;
        } else if (comparator_type == ComparatorType.GE) {
          System.out.println("Condition executeQuery(): comparator_type GE");
          res = left_val.compareTo(right_val) >= 0;
        } else if (comparator_type == ComparatorType.LE) {
          System.out.println("Condition executeQuery(): comparator_type LE");
          res = left_val.compareTo(right_val) <= 0;
        }

        return res;
      }
    }
  }

  // Debug: 少写了一个函数GetResult()

}
