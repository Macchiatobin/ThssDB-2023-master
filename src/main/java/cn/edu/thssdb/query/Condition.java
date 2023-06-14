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

  // Obtain query result recursively
  public boolean executeQuery(QueryRow row) {

    System.out.println("Condition executeQuery(): entered method"); // debug

    // Don't execute queries with null comparisons
    if (left_exp == null || right_exp == null) throw new NullComparisonException();
    else if (left_exp.comparer_type == ComparerType.NULL
        || right_exp.comparer_type == ComparerType.NULL) throw new NullComparisonException();
    else if (left_exp.comparer_value == null || right_exp.comparer_value == null)
      throw new NullComparisonException();
    else {
      System.out.println("Condition executeQuery(): no null comparisons"); // debug
      Comparable left_val = left_exp.comparer_value;
      Comparable right_val = right_exp.comparer_value;
      ComparerType left_type = left_exp.comparer_type;
      ComparerType right_type = right_exp.comparer_type;
      System.out.println("Condition executeQuery(): vals and types assigned"); // debug
      if (left_type == ComparerType.COLUMN) {
        System.out.println("Condition executeQuery(): left_type == ComparerType.COLUMN"); // debug
        Expression left_comparer = row.getColumnComparer((String) left_exp.comparer_value);
        System.out.println("Condition executeQuery(): getColumnComparer() done"); // debug
        left_val = left_comparer.comparer_value;
        left_type = left_comparer.comparer_type;
      }
      if (right_type == ComparerType.COLUMN) {
        System.out.println("Condition executeQuery(): right_type == ComparerType.COLUMN"); // debug
        Expression right_comparer = row.getColumnComparer((String) right_exp.comparer_value);
        right_val = right_comparer.comparer_value;
        right_type = right_comparer.comparer_type;
      }

      // Don't execute queries with null comparisons
      if (left_type == ComparerType.NULL
          || right_type == ComparerType.NULL
          || left_val == null
          || right_val == null) {
        throw new NullComparisonException();
      }

      // Comparison
      if (left_type != right_type) // Different comparer types
      throw new MisMatchTypeException(left_type, right_type);
      else {
        boolean res = false;
        if (comparator_type == ComparatorType.EQ) res = left_val.compareTo(right_val) == 0;
        else if (comparator_type == ComparatorType.NE) res = left_val.compareTo(right_val) != 0;
        else if (comparator_type == ComparatorType.GT) res = left_val.compareTo(right_val) > 0;
        else if (comparator_type == ComparatorType.LT) res = left_val.compareTo(right_val) < 0;
        else if (comparator_type == ComparatorType.GE) res = left_val.compareTo(right_val) >= 0;
        else if (comparator_type == ComparatorType.LE) res = left_val.compareTo(right_val) <= 0;

        return res;
      }
    }
  }

  // Debug: 少写了一个函数GetResult()

}
