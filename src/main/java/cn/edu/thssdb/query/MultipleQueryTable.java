package cn.edu.thssdb.query;

import cn.edu.thssdb.schema.Row;
import cn.edu.thssdb.schema.Table;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

// Class for a joined table

public class MultipleQueryTable extends QueryTable implements Iterator<Row> {

  private ArrayList<Table> tables;
  private ArrayList<Iterator<Row>> row_iterators;
  private MultipleCondition mult_con_for_join; // JOIN逻辑关键
  private LinkedList<Row> rows_to_join;

  public MultipleQueryTable(ArrayList<Table> tables, MultipleCondition mult_con) {
    super();
    this.tables = tables;
    this.mult_con_for_join = mult_con;  // 注意这里是给join表依赖的条件赋值，如果不小心写成了给整个where查询条件赋值貌似会join成笛卡尔积
    this.row_iterators = new ArrayList<>();
    this.columns = new ArrayList<>();
    this.rows_to_join = new LinkedList<>();
    for (Table table : tables) {
      this.columns.addAll(table.columns);
      this.row_iterators.add(table.iterator());
    }
  }

  // find and add the next satisfying row to the queue
  @Override
  public void findAndAddNext() { // JOIN的问题所在？
    while (true) {
      QueryRow cur_row = joinRowsForQuery();
      if (cur_row == null) return;
      // if (multiple_condition == null || multiple_condition.executeQuery(cur_row) == true) { //
      // JOIN问题所在！！！multiple_condition变量混淆！！
      if (mult_con_for_join == null
          || mult_con_for_join.executeQuery(cur_row) == true) { // join表依赖的条件
        if (multiple_condition == null
            || multiple_condition.executeQuery(cur_row) == true) { // 整个查询的where条件
          row_queue.add(cur_row);
          return;
        }
      }
    }
  }

  // Join multiple rows to form a QueryRow
  private QueryRow joinRowsForQuery() {
    if (rows_to_join.isEmpty()) { // no rows to join
      for (Iterator<Row> iter : row_iterators) {
        if (!iter.hasNext()) return null;
        else rows_to_join.push(iter.next());
      }
      return new QueryRow(rows_to_join, tables);
    } else {
      int iter_index;
      for (iter_index = row_iterators.size() - 1; iter_index >= 0; iter_index--) {
        rows_to_join.pop();
        if (!row_iterators.get(iter_index).hasNext())
          row_iterators.set(iter_index, tables.get(iter_index).iterator());
        else break;
      }
      if (iter_index < 0) return null;
      // add rows back
      for (int i = iter_index; i < row_iterators.size(); i++) {
        if (!row_iterators.get(i).hasNext()) return null;
        rows_to_join.push(row_iterators.get(i).next());
      }
      return new QueryRow(rows_to_join, tables);
    }
  }

  // TODO: check metaInfo related
  @Override
  public ArrayList<MetaInfo> GenerateMetaInfo() {
    ArrayList<MetaInfo> the_meta = new ArrayList<>();
    for (Table table : tables) {
      the_meta.add(new MetaInfo(table.tableName, table.columns));
    }
    return the_meta;
  }
}
