package cn.edu.thssdb.query;

import cn.edu.thssdb.schema.Column;

import java.util.ArrayList;
import java.util.List;

class MetaInfo {

  private String tableName;
  private List<Column> columns;

  MetaInfo(String tableName, ArrayList<Column> columns) {
    this.tableName = tableName;
    this.columns = columns;
  }

  int columnFind(String name) {
    /* TODO */
    // v1 done
    for(int i = 0; i < columns.size(); i++) {
      String cur_name = columns.get(i).getName();
      if(cur_name.equals(name)){
        return i;
      }
    }
    return -1;
  }
}
