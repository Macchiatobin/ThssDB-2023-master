package cn.edu.thssdb.query;

import cn.edu.thssdb.exception.UnknownOpTypeException;
import cn.edu.thssdb.type.LogicalOperatorType;

/* MultipleCondition:
 *      Condition, LogicalOperator(AND/OR), Condition
 */

public class MultipleCondition {
  public boolean has_mult_conditions; // whether the condition is composed of multiple conditions

  // Case of a single condition
  public Condition condition;

  // Case of multiple conditions
  public LogicalOperatorType op_type;
  public MultipleCondition left_condition;
  public MultipleCondition right_condition;

  public MultipleCondition(Condition condition) {
    this.has_mult_conditions = false;
    this.condition = condition;
  }

  public MultipleCondition(
      MultipleCondition left, MultipleCondition right, LogicalOperatorType type) {
    this.has_mult_conditions = true;
    this.left_condition = left;
    this.right_condition = right;
    this.op_type = type;
  }

  public String getText() {
    if (!has_mult_conditions) return condition.getText();
    else return "复合条件getText()暂未实现";
  }

  // Obtain query result recursively
  public boolean executeQuery(QueryRow row) {

    //    System.out.println("MultipleCondition executeQuery(): entered method"); // debug

    // Case of a Single Condition
    if (!has_mult_conditions) {
      System.out.println("MultipleCondition executeQuery(): case1 single condition"); // debug
      if (condition == null) {
        System.out.println("MultipleCondition executeQuery(): condition is null"); // debug
        return true;
      }
      return condition.executeQuery(row); // obtain result recursively
    }
    // Case of Multiple Conditions
    else {
      System.out.println("MultipleCondition executeQuery(): case2 multiple conditions"); // debug
      // Obtain results recursively
      boolean left_res = true, right_res = true;
      if (left_condition != null) left_res = left_condition.executeQuery(row);
      if (right_condition != null) right_res = right_condition.executeQuery(row);

      // Calculate final result
      //      System.out.println("MultipleCondition executeQuery(): calculate final result"); //
      // debug
      //      System.out.println("MultipleCondition executeQuery(): op_type \'" + op_type + "\'");
      // // debug
      //      System.out.println(
      //          "MultipleCondition executeQuery(): left_res \'" + left_res + "\'"); // debug
      //      System.out.println(
      //          "MultipleCondition executeQuery(): op_type \'" + right_res + "\'"); // debug
      if (op_type == LogicalOperatorType.AND) return left_res && right_res;
      else if (op_type == LogicalOperatorType.OR) return left_res || right_res;

      throw new UnknownOpTypeException();
    }
  }
}
