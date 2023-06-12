package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.query.MetaInfo;
import cn.edu.thssdb.rpc.thrift.ExecuteStatementResp;
import cn.edu.thssdb.schema.*;
import cn.edu.thssdb.type.ColumnType;
import cn.edu.thssdb.utils.StatusUtil;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.ArrayList;
import java.util.List;

import static cn.edu.thssdb.utils.Global.*;
import static cn.edu.thssdb.utils.Global.COMP_NE;

public class UpdatePlan extends LogicalPlan {

  private String set_attrName;
  private String set_attrValue;
  private String where_attrName;
  private String where_attrValue;
  private String tableName;
  int where_comparator;
  public UpdatePlan(String tableName, String san, String sav,
                    String wan, String wav, String comp) {
    super(LogicalPlanType.UPDATE);
    this.tableName = tableName;
    this.set_attrName = san;
    this.set_attrValue = sav;
    this.where_attrName = wan;
    this.where_attrValue = wav;

    if (comp == "=") this.where_comparator = COMP_EQ;
    else if (comp == ">=") this.where_comparator = COMP_GE;
    else if (comp == ">") this.where_comparator = COMP_GT;
    else if (comp == "<=") this.where_comparator = COMP_LE;
    else if (comp == "<") this.where_comparator = COMP_LT;
    else if (comp == "<>") this.where_comparator = COMP_NE;
    else this.where_comparator = 6; // unknown

    // it seems always do: delete where some_name = some_value;
  }

  @Override
  public String toString() {
    return "UpdatePlan";
  } // TODO

  @Override
  public ExecuteStatementResp execute_plan() {
    try {
      Manager manager = Manager.getInstance();
      Database cur_db = manager.getCurDB();
      if (cur_db == null) {
        return new ExecuteStatementResp(StatusUtil.fail("Use database first."), false);
      }

      Table cur_tb = cur_db.getTable(tableName);
      if (cur_tb == null) {
        return new ExecuteStatementResp(StatusUtil.fail("Table doesn't exist."), false);
      }

      MetaInfo cur_metaInfo = cur_db.metaInfos.get(tableName);
      List<Column> cur_columns = cur_metaInfo.getColumns();
      int set_column_index = cur_metaInfo.columnFind(set_attrName);
      int where_column_index = cur_metaInfo.columnFind(where_attrName);
      Column set_column = cur_columns.get(set_column_index);
      Column where_column = cur_columns.get(where_column_index);
      ColumnType set_column_type = set_column.getType();
      ColumnType where_column_type = where_column.getType();

      if (set_column_index == cur_tb.getPrimaryIndex()) { // set attribute is key
        Row new_key_row = cur_tb.get(new Entry(Table.getColumnTypeValue(set_column_type, set_attrValue)));
        // check if there's already a data with new key
        if (new_key_row != null) {
          throw new KeyAlreadyExistsException();
        }
      }

      Entry entry_to_delete = new Entry(Table.getColumnTypeValue(where_column_type, where_attrValue));
      Row old_row = cur_tb.get(entry_to_delete); // get old row
      ArrayList<Entry> entries = old_row.getEntries();

      ArrayList<Entry> new_entries = new ArrayList<>();
      int it = 0;
      for (Entry e: entries) { // copy into new entry
        new_entries.add(new Entry(Table.getColumnTypeValue(cur_columns.get(it).getType(), e.toString())));
        ++it;
      }
      new_entries.set(set_column_index, new Entry(Table.getColumnTypeValue(set_column_type, set_attrValue)));
      // modify set value

      // create new row with modified entries
      Row new_row = new Row(new_entries);

      cur_tb.update(old_row, new_row);

    } catch (Exception e) {
      return new ExecuteStatementResp(StatusUtil.fail(e.toString()), false);
    }


    return new ExecuteStatementResp(StatusUtil.success(), false);
  }
}
