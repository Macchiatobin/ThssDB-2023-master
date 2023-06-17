package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.parser.MySQLParser;
import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.query.MetaInfo;
import cn.edu.thssdb.query.QueryResult;
import cn.edu.thssdb.rpc.thrift.ExecuteStatementResp;
import cn.edu.thssdb.schema.*;
import cn.edu.thssdb.type.ColumnType;
import cn.edu.thssdb.utils.StatusUtil;

import java.util.*;

import static cn.edu.thssdb.utils.Global.*;

public class DeletePlan extends LogicalPlan {
  private Manager manager;
  public static MySQLParser handler;
  ArrayList<QueryResult> the_result;
  ArrayList<QueryResult> result = new ArrayList<>();

  // added for Ray start
  ArrayList<Row> deleted_rows;
  // added for Ray end

  private String attrName;
  private String attrValue;
  private String tableName;

  private int comparator; // from GLOBAL.COMP_...

  public DeletePlan(String tableName, String attrname, String attrvalue, String comp) {
    super(LogicalPlanType.DELETE);
    this.tableName = tableName;
    this.attrName = attrname; // condition attribute name
    this.attrValue = attrvalue; // condition attribute value
    this.manager = manager;

    // added for Ray start
    this.deleted_rows = new ArrayList<>();
    // added for Ray end

    handler = new MySQLParser(manager);

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
    return "DeletePlan{"
        + "tableName='"
        + tableName
        + '\''
        + ";where attr_name='"
        + attrName
        + "',attr_value='"
        + attrValue
        + "'}";
  }

  public String getAttrName() {
    return attrName;
  }

  public String getAttrValue() {
    return attrValue;
  }

  //  @Override
  //  public ExecuteStatementResp execute_plan(long the_session) {
  //    return null;
  //  }

    @Override
    public ArrayList<String> getTableName() {
        return new ArrayList<>(Collections.singletonList(this.tableName));
    }

    public LinkedList<String> getLog() {
    LinkedList<String> log = new LinkedList<>();
    Database cur_db = Manager.getInstance().getCurDB();
    Table cur_tb = cur_db.getTable(tableName);
    int primaryIndex = cur_tb.getPrimaryIndex();
    for (Row row : deleted_rows) {
      log.add("DELETE FROM " + tableName + " WHERE " + attrName + " = " + attrValue);
    }
    return log;
  }

  @Override
  public ExecuteStatementResp execute_plan() {
    Manager manager = Manager.getInstance();
    Database cur_db = manager.getCurDB();

    // Transaction Lock
    //    if (!manager.transaction_sessions.contains(the_session)) {
    //      System.out.println("Auto Commit:" + the_session);
    //      System.out.println(!manager.transaction_sessions.contains(the_session));
    //      handler.evaluate("AUTO-BEGIN TRANSACTION", the_session);
    //      the_result = handler.evaluate("DELETE", the_session);
    //      result.addAll(the_result);
    //      handler.evaluate("AUTO COMMIT", the_session);
    //
    //    } else {
    //      System.out.println("Commit:" + the_session);
    //      System.out.println(!manager.transaction_sessions.contains(the_session));
    //      the_result = handler.evaluate("DELETE", the_session);
    //      result.addAll(the_result);
    //    }
    //    long session = 0;
    //    if (manager.transaction_sessions.contains(session)) {
    //      Table the_table = cur_db.getTable(tableName);
    //      while (true) {
    //        if (!manager.lockTransactionList.contains(session)) // 新加入一个session
    //        {
    //          int get_lock = the_table.acquireWriteLock(session);
    //          if (get_lock != -1) {
    //            if (get_lock == 1) {
    //              ArrayList<String> tmp = manager.writeLockMap.get(session);
    //              tmp.add(tableName);
    //              manager.writeLockMap.put(session, tmp);
    //            }
    //            break;
    //          } else {
    //            manager.lockTransactionList.add(session);
    //          }
    //        } else // 之前等待的session
    //        {
    //          if (manager.lockTransactionList.get(0) == session) // 只查看阻塞队列开头session
    //          {
    //            int get_lock = the_table.acquireWriteLock(session);
    //            if (get_lock != -1) {
    //              if (get_lock == 1) {
    //                ArrayList<String> tmp = manager.writeLockMap.get(session);
    //                tmp.add(tableName);
    //                manager.writeLockMap.put(session, tmp);
    //              }
    //              manager.lockTransactionList.remove(0);
    //              break;
    //            }
    //          }
    //        }
    //        try {
    //          // System.out.print("session: "+session+": ");
    //          // System.out.println(manager.session_queue);
    //          Thread.sleep(500); // 休眠3秒
    //        } catch (Exception e) {
    //          System.out.println("Got an exception!");
    //        }
    //      }
    //    } else {
    try {
      if (cur_db == null) {
        return new ExecuteStatementResp(StatusUtil.fail("Use database first."), false);
      }
      Table cur_tb = cur_db.getTable(tableName);
      if (cur_tb == null) {
        return new ExecuteStatementResp(StatusUtil.fail("Table doesn't exist."), false);
      }
      MetaInfo cur_metaInfo = cur_db.metaInfos.get(tableName);
      List<Column> cur_columns = cur_metaInfo.getColumns();
      int cur_column_index = cur_metaInfo.columnFind(attrName); // where_attr index
      Column cur_column = cur_columns.get(cur_column_index); // where column
      ColumnType cur_column_type = cur_column.getType(); // where column type
      Entry entry_to_delete = new Entry(Table.getColumnTypeValue(cur_column_type, attrValue));

      if (cur_column_index == cur_tb.getPrimaryIndex()) { // delete on primary key\

        // added for Ray start
        Row temp_row = cur_tb.get(entry_to_delete);
        ArrayList<Entry> entries = new ArrayList<>();
        int it = 0;
        for (Entry e : temp_row.getEntries()) {
          entries.add(
              new Entry(Table.getColumnTypeValue(cur_columns.get(it).getType(), e.toString())));
          ++it;
        }
        this.deleted_rows.add(new Row(entries));
        // added for Ray end

        cur_tb.delete(entry_to_delete); // throw IllegalArgumentException, if key doesn't exist
      } else { // delete on non-primary key
        Iterator<Row> iterator = cur_tb.iterator();
        ArrayList<Entry> entries_to_delete = new ArrayList<>(); // key entries to delete
        while (iterator.hasNext()) {
          Row cur_row = iterator.next();
          ArrayList<Entry> entries = cur_row.getEntries();
          if (entries.get(cur_column_index).equals(entry_to_delete)) { // got row to delete
            entries_to_delete.add(
                new Entry(
                    entries.get(cur_tb.getPrimaryIndex())
                        .value)); // add its primary key to delete list
          }
        }

        for (Entry cur_entry : entries_to_delete) {
          // added for Ray start
          Row temp_row = cur_tb.get(cur_entry);
          ArrayList<Entry> entries = new ArrayList<>();
          int it = 0;
          for (Entry e : temp_row.getEntries()) {
            entries.add(
                new Entry(Table.getColumnTypeValue(cur_columns.get(it).getType(), e.toString())));
            ++it;
          }
          this.deleted_rows.add(new Row(entries));
          // added for Ray end

          cur_tb.delete(cur_entry); // maybe throw some error
        }
      }

    } catch (Exception e) {
      return new ExecuteStatementResp(StatusUtil.fail(e.toString()), false);
    }
    //    }
    return new ExecuteStatementResp(StatusUtil.success(), false);
  }
}
