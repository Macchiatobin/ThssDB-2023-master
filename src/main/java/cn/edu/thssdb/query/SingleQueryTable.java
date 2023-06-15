package cn.edu.thssdb.query;

import cn.edu.thssdb.schema.Row;
import cn.edu.thssdb.schema.Table;
import cn.edu.thssdb.type.ColumnType;

import java.util.ArrayList;
import java.util.Iterator;

// Class for a single table

public class SingleQueryTable extends QueryTable implements Iterator<Row> {
  private Table table;
  private Iterator<Row> iterator;

  public SingleQueryTable(Table table) {
    super();
    this.columns = table.columns;
    this.table = table;
    this.iterator = table.iterator();
  }

  // add the next row directly to the queue (ignoring the select condition)
  private void AddNext() {
    System.out.println("SingleQueryTable AddNext(): added next entry directly!"); // debug
    if (iterator.hasNext()) {
      QueryRow cur_row = new QueryRow(iterator.next(), table);
      row_queue.add(cur_row);
    }
  }

  // find and add the next satisfying row to the queue
  @Override
  public void findAndAddNext() {
    if (multiple_condition == null) { // empty
      System.out.println(
          "SingleQueryTable findAndAddNext(): case 1 - multiple_condition is null"); // debug
      AddNext();
      //      System.out.println("SingleQueryTable findAndAddNext(): AddNext() done"); // debug
      return;
    }
    // discarded speeding up for constant values
    System.out.println(
        "SingleQueryTable findAndAddNext(): case 2 - multiple_condition is not null"); // debug
    executeAddNext();
    //    System.out.println("SingleQueryTable findAndAddNext(): executeAddNext() done"); // debug
  }

  // add next selected row
  private void executeAddNext() {
    System.out.println("SingleQueryTable executeAddNext(): add all satisfying entries!"); // debug
    while (iterator.hasNext()) {
      //      System.out.println("SingleQueryTable executeAddNext(): entered loop"); // debug
      Row cur_row = iterator.next();
      //      System.out.println("SingleQueryTable executeAddNext(): obtained next row iterator");
      QueryRow cur_query_row = new QueryRow(cur_row, table);
      //      System.out.println("SingleQueryTable executeAddNext(): constructed new QueryRow");  //
      // debug
      if (multiple_condition.executeQuery(cur_query_row) != true) { // executeQuery recursively
        //        System.out.println("SingleQueryTable executeAddNext(): loop continue");
        continue;
      }
      row_queue.add(cur_query_row);
      //      System.out.println("SingleQueryTable executeAddNext(): new QueryRow added");  // debug
      break;
    }
  }

  @Override
  public ArrayList<MetaInfo> GenerateMetaInfo() {
    ArrayList<MetaInfo> the_meta = new ArrayList<>();
    the_meta.add(new MetaInfo(table.tableName, table.columns));
    return the_meta;
  }

  // convert an element to a primary key element
  private Comparable convertToPrimary(Comparable val) {
    int primary_index = this.table.getPrimaryIndex();
    ColumnType cur_type = this.table.columns.get(primary_index).getType();
    Comparable converted_val = null;
    if (cur_type == ColumnType.INT) converted_val = ((Number) val).intValue();
    else if (cur_type == ColumnType.DOUBLE) converted_val = ((Number) val).doubleValue();
    else if (cur_type == ColumnType.FLOAT) converted_val = ((Number) val).floatValue();
    else if (cur_type == ColumnType.LONG) converted_val = ((Number) val).longValue();
    else if (cur_type == ColumnType.STRING) converted_val = val + "";

    return converted_val;
  }
}
