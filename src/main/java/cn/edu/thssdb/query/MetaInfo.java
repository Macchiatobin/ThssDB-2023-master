package cn.edu.thssdb.query;

import cn.edu.thssdb.schema.Column;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MetaInfo implements Serializable {

  private String tableName;
  private List<Column> columns;

  public MetaInfo(String tableName, ArrayList<Column> columns) {
    this.tableName = tableName;
    this.columns = columns;
  }

  public int columnFind(String name) {
    /* TODO */
    // v1 done
    for (int i = 0; i < columns.size(); i++) {
      String cur_name = columns.get(i).getName();
      if (cur_name.equals(name)) {
        return i;
      }
    }
    return -1;
  }

  public String getTableName() {
    return this.tableName;
  }

  public String GetFormattedName(int i) {
    if (i < 0 || i >= columns.size()) {
      throw new IndexOutOfBoundsException();
    }
    return tableName + "." + columns.get(i).getName();
  }

  public List<Column> getColumns() {
    return this.columns;
  }

  int getColumnsNum() {
    return columns.size();
  }

  int getColIndex(String name) {
    for (int i = 0; i < columns.size(); i++) {
      if (columns.get(i).getName().equalsIgnoreCase(name)) {
        return i;
      }
    }
    return -1;
  }
}
