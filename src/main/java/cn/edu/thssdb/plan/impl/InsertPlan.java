package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.exception.DuplicateKeyException;
import cn.edu.thssdb.parser.MySQLParser;
import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.query.MetaInfo;
import cn.edu.thssdb.query.QueryResult;
import cn.edu.thssdb.rpc.thrift.ExecuteStatementResp;
import cn.edu.thssdb.schema.*;
import cn.edu.thssdb.type.ColumnType;
import cn.edu.thssdb.utils.StatusUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static cn.edu.thssdb.type.ColumnType.STRING;

public class InsertPlan extends LogicalPlan {
  private Manager manager;
  public static MySQLParser handler;
  ArrayList<QueryResult> the_result;
  ArrayList<QueryResult> result = new ArrayList<>();
  private ArrayList<Row> rowsHasInsert;


  private String tableName;
  private List<String> columnNames;
  private List<String> entryValues;

  public InsertPlan(String tableName, List<String> columnList, List<String> entryList) {
    super(LogicalPlanType.INSERT);
    this.tableName = tableName;
    this.columnNames = columnList;
    this.entryValues = entryList;
    this.rowsHasInsert = new ArrayList<>();

    //    this.manager = manager;
    //    handler = new MySQLParser(manager);
  }

  public ArrayList<String> getTableName() {
    return new ArrayList<>(Collections.singletonList(this.tableName));
  }

  public List<String> getColumnNames() {
    return columnNames;
  }

  public List<String> getEntryValues() {
    return entryValues;
  }

  //  @Override
  //  public ExecuteStatementResp execute_plan(long the_session) {
  //    return null;
  //  }

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

  public LinkedList<String> getLog() {
    LinkedList<String> log = new LinkedList<>();
    for (Row row : rowsHasInsert) {
      log.add("INSERT INTO " + tableName + " VALUES " + "(" + row.toString() + ")");
    }
    return log;
  }

  @Override
  public ExecuteStatementResp execute_plan() {
    System.out.println("INSIDE INSERT PLAN");
    Manager manager = Manager.getInstance();
    Database dbForInsert = manager.getCurDB();
    if (dbForInsert == null) {
      return new ExecuteStatementResp(StatusUtil.fail("Use database first."), false);
    }
    Table tableToInsert = dbForInsert.getTable(tableName);
    if (tableToInsert == null) {
      return new ExecuteStatementResp(StatusUtil.fail("Table doesn't exist!"), false);
    }
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


    // Transaction Lock
    //    if (!manager.transaction_sessions.contains(the_session)) {
    //      System.out.println("Auto Commit:" + the_session);
    //      System.out.println(!manager.transaction_sessions.contains(the_session));
    //      handler.evaluate("AUTO-BEGIN TRANSACTION", the_session);
    //      the_result = handler.evaluate("INSERT", the_session);
    //      result.addAll(the_result);
    //      handler.evaluate("AUTO COMMIT", the_session);
    //
    //    } else {
    //      System.out.println("Commit:" + the_session);
    //      System.out.println(!manager.transaction_sessions.contains(the_session));
    //      the_result = handler.evaluate("INSERT", the_session);
    //      result.addAll(the_result);
    //    }
    //
    //    long session = 0;
    //    if (manager.transaction_sessions.contains(session)) {
    //      Table the_table = dbForInsert.getTable(tableName);
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
      dbForInsert.getTable(tableName).insert(rowToInsert);
      this.rowsHasInsert.add(rowToInsert);

    } catch (DuplicateKeyException e) {
      return new ExecuteStatementResp(StatusUtil.fail(e.getMessage()), false);
    }
    //    }

    return new ExecuteStatementResp(StatusUtil.success(), false);
  }
}
