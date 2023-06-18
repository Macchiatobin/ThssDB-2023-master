package cn.edu.thssdb.query;

import cn.edu.thssdb.schema.Column;
import cn.edu.thssdb.schema.Row;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

public abstract class QueryTable implements Iterator<Row> {

  MultipleCondition multiple_condition;  // WHERE给出的查询条件

  public ArrayList<Column> columns;  // 查询表的列
  LinkedList<QueryRow> row_queue;  // 查询表的行
  boolean first_flag = true;  // 查询表的列

  QueryTable() {
    this.row_queue = new LinkedList<>();
  }

  public abstract void findAndAddNext(); // implemented in children classes

  public abstract ArrayList<MetaInfo> GenerateMetaInfo();

  @Override
  public boolean hasNext() {
    if (!row_queue.isEmpty() || first_flag) return true;
    return false;
  }

  // return next row
  @Override
  public QueryRow next() {
    if (row_queue.isEmpty()) {
      findAndAddNext();
      System.out.println("QueryTable next(): row_queue.isEmpty() -> findAndAddNext done"); // debug
      if (first_flag) first_flag = false;
    }

    QueryRow res_row = null;
    if (!row_queue.isEmpty()) res_row = row_queue.poll();
    else return null;
    if (row_queue.isEmpty()) findAndAddNext();

    System.out.println("QueryTable next(): !row_queue.isEmpty() -> findAndAddNext done"); // debug

    return res_row;
  }

  public void setMultipleCondition(MultipleCondition multCon) {
    this.multiple_condition = multCon;
  }

  public MultipleCondition getMultipleCondition() {
    return this.multiple_condition;
  }
}
