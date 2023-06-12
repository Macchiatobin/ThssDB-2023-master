package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.exception.NoTableSelectedException;
import cn.edu.thssdb.exception.QueryResultException;
import cn.edu.thssdb.exception.UnknownOperatorException;
import cn.edu.thssdb.parser.MySQLParser;
import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.query.*;
import cn.edu.thssdb.rpc.thrift.ExecuteStatementResp;
import cn.edu.thssdb.schema.*;
import cn.edu.thssdb.sql.SQLParser;
import cn.edu.thssdb.type.*;
import cn.edu.thssdb.utils.StatusUtil;

import java.util.ArrayList;
import java.util.List;

public class SelectPlan extends LogicalPlan {

  private SQLParser.SelectStmtContext ctx;
  private Database cur_db;
  private Manager manager;
  public static MySQLParser my_parser;

  public SelectPlan(SQLParser.SelectStmtContext ctx, Database cur_db, Manager manager) {
    super(LogicalPlan.LogicalPlanType.SELECT);
    this.ctx = ctx;
    this.cur_db = cur_db;
    this.manager = manager;
    this.my_parser = new MySQLParser(this.manager);
  }

  @Override
  public String toString() {
    return "SelectPlan";
  }

  // Construct QueryTable
  public QueryTable createQueryTable(SQLParser.TableQueryContext ctx) {

    // Single query table
    if (ctx.K_JOIN().size() == 0) {
      return cur_db.createSingleQueryTable(ctx.tableName(0).getText().toLowerCase());
    }
    // Joined query table
    else {
      MultipleCondition multiple_condition = obtainMultipleCondition(ctx.multipleCondition());
      ArrayList<String> table_names = new ArrayList<>();
      for (SQLParser.TableNameContext subCtx : ctx.tableName()) {
        table_names.add(subCtx.getText().toLowerCase());
      }
      return cur_db.createJoinedQueryTable(table_names, multiple_condition);
    }
  }

  @Override
  public ExecuteStatementResp execute_plan() {
    /* TODO */

    boolean is_distinct;
    if (ctx.K_DISTINCT() != null) is_distinct = true;
    else is_distinct = false;
    int col_num = ctx.resultColumn().size();
    String[] col_names = new String[col_num];

    /* "SELECT": Obtain selected column names */
    for (int i = 0; i < col_num; i++) {
      String cur_col_name = ctx.resultColumn(i).getText().toLowerCase();
      if (cur_col_name.equals("*")) {
        col_names = null;
        break;
      }
      col_names[i] = cur_col_name;
    }

    System.out.println("SELECT done"); // debug

    /* "FROM": Obtain selected table names and create QueryTable */
    int query_num = ctx.tableQuery().size();
    if (query_num == 0) {
      throw new NoTableSelectedException();
    }
    QueryTable query_table = null;
    ArrayList<String> table_names = new ArrayList<>();
    try {
      System.out.println("Table names: " + ctx.tableQuery()); // for debugging
      query_table = createQueryTable(ctx.tableQuery(0));
      for (SQLParser.TableNameContext cur_ctx : ctx.tableQuery(0).tableName()) {
        System.out.println("Table name: " + cur_ctx.getText().toLowerCase()); // for debugging
        table_names.add(cur_ctx.getText().toLowerCase());
      }
    } catch (Exception e) {
      throw new QueryResultException();
    }
    if (query_table == null) {
      throw new NoTableSelectedException();
    }

    System.out.println("FROM done"); // debug

    /* "WHERE": obtain query conditions */
    SQLParser.MultipleConditionContext mult_con_ctx = ctx.multipleCondition();
    MultipleCondition multiple_condition = null;
    if (mult_con_ctx == null) { // select statement with no "where" clause
      System.out.println("no WHERE statement: mult_con_ctx is null"); // debug
    } else {
      multiple_condition = obtainMultipleCondition(mult_con_ctx); // obtain results recursively
      System.out.println("WHERE recursive part done"); // debug
    }

    // TODO: 和transaction交互

    QueryResult query_res = null;
    // execute select
    try {
      // debug: check null
      if (query_table == null) System.out.println("query_table is null!");
      if (col_names == null) System.out.println("col_names is null!(Correct if \'select *\')");
      if (multiple_condition == null)
        System.out.println("multiple_condition is null!(Correct if no \'WHERE\' clause)");

      // debug: check isDistinct
      if (is_distinct) System.out.println("isDistinct: true");
      else System.out.println("isDistinct: false");

      query_res = cur_db.select(query_table, col_names, multiple_condition, is_distinct);
      System.out.println("WHERE select execution done"); // debug
    } catch (Exception e) {
      throw new QueryResultException(); // 但是这样好像会把更细节的报错也都返回成QueryResultException
    }

    // build return result
    ExecuteStatementResp statement_res = new ExecuteStatementResp(StatusUtil.success(), true);

    if (query_res == null) // 按理来说不该是null
    throw new QueryResultException();

    ArrayList<Row> row_results = query_res.getRows(); // 要考虑没有row的情况，按理来说应该是返回一个长度为0的arraylist
    System.out.println(
        "SelectPlan executePlan(): row_results.size(): " + row_results.size()); // debug
    List<String> column_names = query_res.getColNames();
    for (String col_name : column_names) {
      statement_res.addToColumnsList(col_name);
    }
    for (Row row : row_results) { // 有row的情况
      List<String> cur_row_toStr = new ArrayList<>();
      ArrayList<Entry> cur_row_entries = row.getEntries();
      for (Entry entry : cur_row_entries) {
        cur_row_toStr.add(entry.toString());
      }
      statement_res.addToRowList(cur_row_toStr);
    }
    if (row_results.size() == 0) { // 无row的情况，也需要返回一个空的list，否则会报错
      List<String> null_row = new ArrayList<>();
      statement_res.addToRowList(null_row);
    }

    return statement_res;
  }

  public MultipleCondition obtainMultipleCondition(
      SQLParser.MultipleConditionContext mult_con_ctx) {
    //    SQLParser.ConditionContext con_ctx = mult_con_ctx.condition();

    // Single condition
    if (mult_con_ctx.AND() == null
        && mult_con_ctx.OR() == null) { // original: if (mult_con_ctx.condition() != null)
      System.out.println("SelectPlan obtainMultipleCondition(): single condition");
      return new MultipleCondition(obtainCondition(mult_con_ctx.condition()));
    }
    // Multiple conditions
    else {
      System.out.println("SelectPlan obtainMultipleCondition(): multiple condition");
      LogicalOperatorType logical_op_type;
      if (mult_con_ctx.OR() != null) {
        logical_op_type = LogicalOperatorType.OR;
      } else if (mult_con_ctx.AND() != null) {
        logical_op_type = LogicalOperatorType.AND;
      } else {
        throw new UnknownOperatorException();
      }
      return new MultipleCondition(
          obtainMultipleCondition(mult_con_ctx.multipleCondition(0)),
          obtainMultipleCondition(mult_con_ctx.multipleCondition(1)),
          logical_op_type); // 做递归，可能有bug
    }
  }

  public Condition obtainCondition(SQLParser.ConditionContext ctx) {
    Expression left = obtainExpression(ctx.expression(0));
    Expression right = obtainExpression(ctx.expression(1));
    ComparatorType type = obtainComparator(ctx.comparator());
    return new Condition(left, right, type);
  }

  public ComparatorType obtainComparator(SQLParser.ComparatorContext ctx) {
    if (ctx.EQ() != null) return ComparatorType.EQ;
    else if (ctx.NE() != null) return ComparatorType.NE;
    else if (ctx.GT() != null) return ComparatorType.GT;
    else if (ctx.LT() != null) return ComparatorType.LT;
    else if (ctx.GE() != null) return ComparatorType.GE;
    else if (ctx.LE() != null) return ComparatorType.LE;

    return null;
  }

  // In our implementation, expression = comparer
  public Expression obtainExpression(SQLParser.ExpressionContext ctx) {
    if (ctx.comparer() != null) return obtainComparer(ctx.comparer());
    else return null;
  }

  public Expression obtainComparer(SQLParser.ComparerContext ctx) {
    // 处理column情况
    if (ctx.columnFullName() != null)
      return new Expression(ComparerType.COLUMN, ctx.columnFullName().getText());

    // 获得类型和内容
    LiteralValueType type = obtainLiteralValue(ctx.literalValue());
    String text = ctx.literalValue().getText();
    if (type == LiteralValueType.NUMBER) return new Expression(ComparerType.NUMBER, text);
    else if (type == LiteralValueType.STRING)
      return new Expression(ComparerType.STRING, text.substring(1, text.length() - 1));
    else if (type == LiteralValueType.NULL) return new Expression(ComparerType.NULL, null);
    else return null;
  }

  public LiteralValueType obtainLiteralValue(SQLParser.LiteralValueContext ctx) {
    if (ctx.NUMERIC_LITERAL() != null) return LiteralValueType.NUMBER;
    else if (ctx.STRING_LITERAL() != null) return LiteralValueType.STRING;
    else if (ctx.K_NULL() != null) return LiteralValueType.NULL;

    return null;
  }

  public String[] obtainConstraints(SQLParser.TableConstraintContext ctx) {
    int n = ctx.columnName().size();
    String[] composite_names = new String[n];
    for (int i = 0; i < n; i++) {
      composite_names[i] = ctx.columnName(i).getText().toLowerCase();
    }
    return composite_names;
  }

  public String[] ObtainEntryValue(SQLParser.ValueEntryContext ctx) {
    String[] vals = new String[ctx.literalValue().size()];
    for (int i = 0; i < ctx.literalValue().size(); i++) vals[i] = ctx.literalValue(i).getText();

    return vals;
  }
}
