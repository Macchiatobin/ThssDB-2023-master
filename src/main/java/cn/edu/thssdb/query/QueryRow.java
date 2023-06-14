package cn.edu.thssdb.query;

import cn.edu.thssdb.exception.ColumnInMoreThanOneTableException;
import cn.edu.thssdb.exception.ColumnNotFoundException;
import cn.edu.thssdb.exception.InvalidColumnNameException;
import cn.edu.thssdb.schema.Row;
import cn.edu.thssdb.schema.Table;
import cn.edu.thssdb.type.ColumnType;
import cn.edu.thssdb.type.ComparerType;

import java.util.ArrayList;
import java.util.LinkedList;

// Multiple rows joined for query

public class QueryRow extends Row {
  private ArrayList<Table> query_tables;

  public QueryRow(LinkedList<Row> rows, ArrayList<Table> tables) {
    super();
    query_tables = new ArrayList<>();
    this.entries = new ArrayList<>();

    for (int i = rows.size() - 1; i >= 0; i--) entries.addAll(rows.get(i).getEntries());
    for (Table tb : tables) query_tables.add(tb);
  }

  public QueryRow(Row row, Table table) {
    super();
    query_tables = new ArrayList<>();
    query_tables.add(table);
    this.entries = new ArrayList<>();
    entries.addAll(row.getEntries());
  }

  // return the comparer of column col_name
  public Expression getColumnComparer(String col_name) {

    System.out.println("QueryRow getColumnComparer(): entered method"); // debug

    ComparerType comparer_type = ComparerType.NULL;
    ColumnType col_type = ColumnType.INT;

    int index = 0;

    // formatted as "columnName"
    if (!col_name.contains(".")) {
      System.out.println("QueryRow getColumnComparer(): formatted as \"columnName\""); // debug
      int num_hits = 0;
      int total_index = 0;
      for (int i = 0; i < query_tables.size(); i++) {
        System.out.println("QueryRow getColumnComparer(): entered i loop " + i); // debug
        Table cur_table = query_tables.get(i);
        System.out.println("QueryRow getColumnComparer(): obtained cur_table"); // debug
        for (int j = 0; j < cur_table.columns.size(); j++) {
          System.out.println("QueryRow getColumnComparer(): entered j loop " + j); // debug
          System.out.println("QueryRow getColumnComparer(): col_name: " + col_name); // debug
          System.out.println(
              "QueryRow getColumnComparer(): "
                  + "cur_table.columns.get(j).getName()): "
                  + cur_table.columns.get(j).getName()); // debug
          // remember to convert to lower case before comparing
          if (col_name.equalsIgnoreCase(cur_table.columns.get(j).getName())) {
            System.out.println(
                "QueryRow getColumnComparer(): column names \'" + col_name + "\' equal"); // debug
            num_hits++;
            index = total_index + j;
            col_type = cur_table.columns.get(j).getType();
          }
        }
        total_index += cur_table.columns.size();
      }
      System.out.println("QueryRow getColumnComparer(): num_hits = " + num_hits); // debug
      if (num_hits < 1) {
        System.out.println(
            "QueryRow getColumnComparer(): num_hits < 1, throw ColumnNotFoundException"); // debug
        throw new ColumnNotFoundException(col_name);
      } else if (num_hits > 1) {
        System.out.println(
            "QueryRow getColumnComparer(): num_hits > 1, throw ColumnInMoreThanOneTableException"); // debug
        throw new ColumnInMoreThanOneTableException(col_name);
      }
    }
    // formatted as "tableName.columnName"
    else {
      System.out.println(
          "QueryRow getColumnComparer(): formatted as \"tableName.columnName\""); // debug
      String[] sep_names = separateColumnName(col_name);
      System.out.println("QueryRow getColumnComparer(): separateColumnName() done"); // debug
      String table_name = sep_names[0];
      String entry_name = sep_names[1];
      int total_index = 0;
      boolean is_found = false;
      for (Table table : query_tables) {
        System.out.println("QueryRow getColumnComparer(): entered table search loop"); // debug
        if (table_name.equalsIgnoreCase(table.tableName)) {
          System.out.println("QueryRow getColumnComparer(): matching table found"); // debug
          for (int j = 0; j < table.columns.size(); j++) {
            if (entry_name.equalsIgnoreCase(table.columns.get(j).getName())) {
              System.out.println("QueryRow getColumnComparer(): matching entry found"); // debug
              is_found = true;
              index = total_index + j;
              col_type = table.columns.get(j).getType();
              break;
            }
          }
          break;
        }
        total_index += table.columns.size();
      }
      if (!is_found) {
        System.out.println(
            "QueryRow getColumnComparer(): no matching entry found, throw ColumnNotFoundException"); // debug
        throw new ColumnNotFoundException(col_name);
      }
    }
    comparer_type = columnToComparer(col_type);
    Comparable comparer_value = this.entries.get(index).value;

    if (comparer_value == null) {
      return new Expression(ComparerType.NULL, null);
    }
    Expression cur_comparer = new Expression(comparer_type, "" + comparer_value);
    return cur_comparer;
  }

  // convert ColumnType to ComparerType
  private ComparerType columnToComparer(ColumnType col_type) {
    if (col_type == ColumnType.LONG
        || col_type == ColumnType.FLOAT
        || col_type == ColumnType.INT
        || col_type == ColumnType.DOUBLE) return ComparerType.NUMBER;
    else if (col_type == ColumnType.STRING) return ComparerType.STRING;

    return ComparerType.NULL;
  }

  // Separate formatted column name from TableName.ColumnName to an array of TableName,ColumnName
  private String[] separateColumnName(String name) {
    String[] sep_name = name.split("\\.");
    if (sep_name.length != 2) {
      throw new InvalidColumnNameException(name);
    }
    return sep_name;
  }
}
