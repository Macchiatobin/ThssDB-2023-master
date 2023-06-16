package cn.edu.thssdb.query;

import cn.edu.thssdb.exception.ColumnInMoreThanOneTableException;
import cn.edu.thssdb.exception.ColumnNotFoundException;
import cn.edu.thssdb.exception.InvalidColumnNameException;
import cn.edu.thssdb.schema.Entry;
import cn.edu.thssdb.schema.Row;
import cn.edu.thssdb.utils.Cell;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class QueryResult {

  private List<MetaInfo> metaInfoInfos;
  private List<Integer> col_indexes;
  private List<String> col_names;
  private List<Cell> attrs; // what for?
  private QueryTable table;
  private boolean isDistinct;
  private HashSet<String> hash_set;
  private ArrayList<Row> results;
  public boolean mWhetherRight;
  public String mErrorMessage;

  public QueryResult( // join问题所在
      QueryTable queryTable, boolean isDistinct, String[] columns) { // originally QueryTable[]
    /* TODO */
    // v1 done
    this.col_indexes = new ArrayList<>();
    this.col_names = new ArrayList<>();
    this.attrs = new ArrayList<>();
    this.hash_set = new HashSet<>();
    this.results = new ArrayList<>();
    this.metaInfoInfos = new ArrayList<>();
    this.table = queryTable;
    this.isDistinct = isDistinct;
    this.mWhetherRight = true;
    this.mErrorMessage = "";
    //    System.out.println("QueryResult QueryResult(): variable initializations done"); // debug
    this.metaInfoInfos.addAll(this.table.GenerateMetaInfo());
    //    System.out.println("QueryResult QueryResult(): add metaInfoInfos done"); // debug
    setColumns(columns); // select columns
    //    System.out.println("QueryResult QueryResult(): set columns done"); // debug
  }

  public QueryResult(String errorMessage) {
    mWhetherRight = false;
    mErrorMessage = errorMessage;
  }

  public ArrayList<Row> getRows() {
    return results;
  }

  public List<String> getColNames() {
    return col_names;
  }

  private void setColumns(String[] columns) {
    // didn't select any columns -> return all columns
    if (columns == null) {
      //      System.out.println("QueryResult setColumns(): columns is null"); // debug
      int begin_index = 0;
      for (MetaInfo metaInfo : metaInfoInfos) {
        for (int i = 0; i < metaInfo.getColumnsNum(); i++) {
          String full_name = metaInfo.GetFormattedName(i);
          this.col_indexes.add(begin_index + i);
          this.col_names.add(full_name);
        }
        begin_index += metaInfo.getColumnsNum();
      }
    }
    // return selected columns
    else {
      //      System.out.println("QueryResult setColumns(): columns is not null"); // debug
      for (String col_name : columns) { // 需考虑大小写转换
        System.out.println(col_name);
        System.out.println(getColIndex(col_name));
        this.col_indexes.add(getColIndex(col_name));
        this.col_names.add(col_name);
      }
    }
  }

  public int getColIndex(String col_name) {
    // 需考虑大小写转换
    // formatted as "columnname" only
    int index = 0;
    if (!col_name.contains(".")) {
      int num_hits = 0;
      int total_col_index = 0;
      for (int i = 0; i < metaInfoInfos.size(); i++) {
        int cur_col_index = metaInfoInfos.get(i).getColIndex(col_name);
        if (cur_col_index >= 0) {
          num_hits++;
          index = cur_col_index + total_col_index;
        }
        total_col_index += metaInfoInfos.get(i).getColumnsNum();
      }
      if (num_hits < 1) {
        throw new ColumnNotFoundException(col_name);
      } else if (num_hits > 1) {
        throw new ColumnInMoreThanOneTableException(col_name);
      }
    }
    // formatted as "tablename.columnname"
    else {
      // split formatted name
      String[] separated_name = col_name.split("\\.");
      if (separated_name.length != 2) {
        throw new InvalidColumnNameException(col_name);
      }

      // get column index
      String table_name = separated_name[0];
      String attr_name = separated_name[1];
      boolean isFound = false;
      int total_index = 0;
      for (int i = 0; i < metaInfoInfos.size(); i++) {
        String current_name = metaInfoInfos.get(i).getTableName();
        if (!current_name.equals(table_name)) {
          total_index += metaInfoInfos.get(i).getColumnsNum();
          continue;
        }
        int current_index = metaInfoInfos.get(i).getColIndex(attr_name);
        if (current_index >= 0) {
          isFound = true;
          index = current_index + total_index;
          break;
        }
      }
      if (isFound == false) throw new ColumnNotFoundException(col_name);
    }

    return index;
  }

  // Execute query, add all results to results list and hashmap (if distinct)
  public void obtainResults() { // join的问题所在
    while (table.hasNext()) {
      //      System.out.println("QueryResult obtainResults(): entered method"); // debug
      QueryRow cur_row = table.next(); // 关键函数，obtain next row
      //      System.out.println("QueryResult obtainResults(): obtained cur_row"); // debug
      if (cur_row == null) break;
      //      System.out.println("QueryResult obtainResults(): cur_row not null");  // debug
      Entry[] full_entries = new Entry[col_indexes.size()];
      ArrayList<Entry> cur_entries = cur_row.getEntries();
      //      System.out.println("QueryResult obtainResults(): entries initialized");  // debug
      for (int i = 0; i < col_indexes.size(); i++) {
        int index = col_indexes.get(i);
        full_entries[i] = cur_entries.get(index);
      }
      //      System.out.println("QueryResult obtainResults(): entries obtained");  // debug
      Row row = new Row(full_entries);
      //      System.out.println("QueryResult obtainResults(): answer row constructed");  // debug
      String row_str = row.toString();
      if (!hash_set.contains(row_str) || !isDistinct) {
        results.add(row);
        if (isDistinct) {
          hash_set.add(row_str);
        }
      }
      //      System.out.println("QueryResult obtainResults(): 1 answer row added");  // debug
    }
  }

  public QueryTable getTable() {
    return this.table;
  }

  // TODO: 展示MetaInfo相关?

  // original functions
  //  public static Row combineRow(LinkedList<Row> rows) {
  //    // TODO
  //    return null;
  //  }
  //
  //  public Row generateQueryRecord(Row row) {
  //    // TODO
  //    return null;
  //  }

}
