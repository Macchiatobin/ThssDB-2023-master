package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.query.MetaInfo;
import cn.edu.thssdb.rpc.thrift.ExecuteStatementResp;
import cn.edu.thssdb.schema.*;
import cn.edu.thssdb.type.ColumnType;
import cn.edu.thssdb.utils.StatusUtil;

import java.util.List;

import static cn.edu.thssdb.utils.Global.*;

public class DeletePlan extends LogicalPlan {

  private String attrName;
  private String attrValue;
  private String tableName;

  private int comparator; // from GLOBAL.COMP_...

  public DeletePlan(String tableName, String attrname, String attrvalue, String comp) {
    super(LogicalPlanType.DELETE);
    this.tableName = tableName;
    this.attrName = attrname; // condition attribute name
    this.attrValue = attrvalue; // condition attribute value


    if (comp == "=") this.comparator = COMP_EQ;
    else if (comp == ">=") this.comparator = COMP_GE;
    else if (comp == ">") this.comparator = COMP_GT;
    else if (comp == "<=") this.comparator = COMP_LE;
    else if (comp == "<") this.comparator = COMP_LT;
    else if (comp == "<>") this.comparator = COMP_NE;
    else this.comparator = 6; // unknown

    // it seems always do: delete where some_name = some_value;
  }

  @Override
  public String toString() {
    return "DeletePlan{" + "tableName='" + tableName + '\'' + ";where attr_name='" +
            attrName + "',attr_value='" + attrValue + "'}";
  }

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
      int cur_column_index = cur_metaInfo.columnFind(attrName);
      Column cur_column = cur_columns.get(cur_column_index);
      ColumnType cur_column_type = cur_column.getType();
      Entry entry_to_delete = new Entry(Table.getColumnTypeValue(cur_column_type, attrValue));
      cur_tb.delete(entry_to_delete);
    } catch (Exception e) {
      return new ExecuteStatementResp(StatusUtil.fail(e.toString()), false);
    }
    return new ExecuteStatementResp(StatusUtil.success(), false);
  }
}
