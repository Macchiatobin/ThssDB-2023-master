package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.query.MetaInfo;
import cn.edu.thssdb.rpc.thrift.ExecuteStatementResp;
import cn.edu.thssdb.schema.*;
import cn.edu.thssdb.type.ColumnType;
import cn.edu.thssdb.utils.StatusUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static cn.edu.thssdb.type.ColumnType.STRING;

public class InsertPlan extends LogicalPlan {

  private String tableName;
  private List<String> columnNames;
  private List<String> entryValues;

  public InsertPlan(String tableName, List<String> columnList, List<String> entryList) {
    super(LogicalPlanType.INSERT);
    this.tableName = tableName;
    this.columnNames = columnList;
    this.entryValues = entryList;
  }

  public String getTableName() {
    return tableName;
  }

  public List<String> getColumnNames() {
    return columnNames;
  }

  public List<String> getEntryValues() {
    return entryValues;
  }

  @Override
  public String toString() {
    return "InsertPlan{"
        + "tableName='"
        + tableName
        + '\''
        + "; columnNames="
        + columnNames
        + "; entryValues="
        + entryValues
        + '}';
  }

  @Override
  public ExecuteStatementResp execute_plan() {
    Manager manager = Manager.getInstance();
    Database dbForInsert = manager.getCurDB();
    Table tableToInsert = dbForInsert.getTable(tableName);
    MetaInfo metaInfo = dbForInsert.metaInfos.get(tableName);

    int columnNamesSize = columnNames.size(); // need check if 0
    int entryValuesSize = entryValues.size();

    ArrayList<Column> columns = tableToInsert.columns; // columns
    if (entryValuesSize != columns.size()) { // entry value size doesn't match column size
      return new ExecuteStatementResp(StatusUtil.fail("Input entry match failed."), false);
    }
    ArrayList<Entry> entries = new ArrayList<>(Collections.nCopies(columns.size(), null));

    // Constructing Row object to insert
    if (columnNamesSize == 0) { // in order of original column order
      int current_entry_index = 0;
      for (Column c : columns) {
        if (c.getType() == STRING) {
          String cur_string_value = entryValues.get(current_entry_index);
          String new_string_value = cur_string_value.substring(1, cur_string_value.length() - 1);
          entryValues.set(current_entry_index, new_string_value);
        }
        entries.set(
            current_entry_index,
            new Entry(Table.getColumnTypeValue(c.getType(), entryValues.get(current_entry_index))));
        current_entry_index += 1;
      }
    } else { // may not be in order of original column order
      // current_entry_index: index for entryValues
      for (int current_entry_index = 0;
          current_entry_index < entryValues.size();
          ++current_entry_index) {
        int column_index = metaInfo.columnFind(columnNames.get(current_entry_index));
        ColumnType current_Type = columns.get(column_index).getType(); // current_entry column type

        if (current_Type == STRING) { // delete quote
          String cur_string_value = entryValues.get(current_entry_index);
          String new_string_value = cur_string_value.substring(1, cur_string_value.length() - 1);
          entryValues.set(current_entry_index, new_string_value);
        }
        entries.set(
            column_index,
            new Entry(
                Table.getColumnTypeValue(current_Type, entryValues.get(current_entry_index))));
      }
    }
    Row rowToInsert = new Row(entries);
    dbForInsert.getTable(tableName).insert(rowToInsert);

    return new ExecuteStatementResp(StatusUtil.success(), false);
  }
}
