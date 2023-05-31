package cn.edu.thssdb.service;

import cn.edu.thssdb.plan.LogicalGenerator;
import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.plan.impl.*;
import cn.edu.thssdb.query.MetaInfo;
import cn.edu.thssdb.rpc.thrift.ConnectReq;
import cn.edu.thssdb.rpc.thrift.ConnectResp;
import cn.edu.thssdb.rpc.thrift.DisconnectReq;
import cn.edu.thssdb.rpc.thrift.DisconnectResp;
import cn.edu.thssdb.rpc.thrift.ExecuteStatementReq;
import cn.edu.thssdb.rpc.thrift.ExecuteStatementResp;
import cn.edu.thssdb.rpc.thrift.GetTimeReq;
import cn.edu.thssdb.rpc.thrift.GetTimeResp;
import cn.edu.thssdb.rpc.thrift.IService;
import cn.edu.thssdb.rpc.thrift.Status;
import cn.edu.thssdb.schema.*;
import cn.edu.thssdb.type.ColumnType;
import cn.edu.thssdb.utils.Global;
import cn.edu.thssdb.utils.StatusUtil;
import org.apache.thrift.TException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.edu.thssdb.type.ColumnType.STRING;
import static cn.edu.thssdb.utils.Global.DATA_DIR;

public class IServiceHandler implements IService.Iface {

  private Manager manager;
  private static final AtomicInteger sessionCnt = new AtomicInteger(0);

  public IServiceHandler() {
    this.manager = Manager.getInstance();

    // Create Data File Directory
    String dataPath = DATA_DIR;
    File dataFile = new File(dataPath);
    if (!dataFile.exists()) {
      try {
        boolean created = dataFile.mkdir();
        if (!created) throw new IOException();
      } catch (IOException e) {
        // TODO: error handling
        System.out.println("Data File Creation Failed!");
      }
    }
  }

  @Override
  public GetTimeResp getTime(GetTimeReq req) throws TException {
    GetTimeResp resp = new GetTimeResp();
    resp.setTime(new Date().toString());
    resp.setStatus(new Status(Global.SUCCESS_CODE));
    return resp;
  }

  @Override
  public ConnectResp connect(ConnectReq req) throws TException {
    return new ConnectResp(StatusUtil.success(), sessionCnt.getAndIncrement());
  }

  @Override
  public DisconnectResp disconnect(DisconnectReq req) throws TException {
    return new DisconnectResp(StatusUtil.success());
  }

  @Override
  public ExecuteStatementResp executeStatement(ExecuteStatementReq req) throws TException {
    if (req.getSessionId() < 0) {
      return new ExecuteStatementResp(
          StatusUtil.fail("You are not connected. Please connect first."), false);
    }
    // TODO: implement execution logic，需要实现日志记录等
    LogicalPlan plan = LogicalGenerator.generate(req.statement);
    switch (plan.getType()) {
      case CREATE_DB:
        System.out.println("CREATE_DB");
        System.out.println("[DEBUG] " + plan);

        CreateDatabasePlan createDatabasePlan = (CreateDatabasePlan) plan;
        manager.createDatabaseIfNotExists(createDatabasePlan.getDatabaseName());

        return new ExecuteStatementResp(StatusUtil.success(), false);

      case DROP_DB:
        System.out.println("DROP_DB");
        System.out.println("[DEBUG] " + plan);

        DropDatabasePlan dropDatabasePlan = (DropDatabasePlan) plan;
        manager.deleteDatabase(dropDatabasePlan.getDatabaseName());

        return new ExecuteStatementResp(StatusUtil.success(), false);

      case USE_DB:
        System.out.println("USE_DB");
        System.out.println("[DEBUG] " + plan);

        UseDatabasePlan useDatabasePlan = (UseDatabasePlan) plan;
        manager.switchDatabase(useDatabasePlan.getDatabaseName());

        return new ExecuteStatementResp(StatusUtil.success(), false);

      case CREATE_TABLE:
        System.out.println("CREATE_TABLE");
        System.out.println("[DEBUG] " + plan);

        CreateTablePlan createTablePlan = (CreateTablePlan) plan; // downgrading
        Database dbForTableCreate = manager.getCurDB();
        if (dbForTableCreate == null) {
          return new ExecuteStatementResp(StatusUtil.fail("Use database first."), false);
        }
        List<Column> cList = createTablePlan.getColumns();
        dbForTableCreate.create(
            createTablePlan.getTableName(), cList.toArray(new Column[cList.size()]));

        return new ExecuteStatementResp(StatusUtil.success(), false);

      case DROP_TABLE:
        System.out.println("DROP_TABLE");
        System.out.println("[DEBUG] " + plan);

        DropTablePlan dropTablePlan = (DropTablePlan) plan;
        Database dbForTableDrop = manager.getCurDB();
        if (dbForTableDrop == null) {
          return new ExecuteStatementResp(StatusUtil.fail("Use database first."), false);
        }
        dbForTableDrop.drop(dropTablePlan.getTableName());

        return new ExecuteStatementResp(StatusUtil.success(), false);

      case QUIT:
        System.out.println("QUIT");
        System.out.println("[DEBUG] " + plan);

        // TODO: wtf does this quit mean

        return new ExecuteStatementResp(StatusUtil.success(), false);

      case SHOW_TABLE: // SHOW DATABASE tableName
        System.out.println("SHOW_TABLE");
        System.out.println("[DEBUG] " + plan);
        return new ExecuteStatementResp(StatusUtil.success(), false);

      case AUTO_COMMIT:
        System.out.println("AUTO_COMMIT");
        System.out.println("[DEBUG] " + plan);

        // TODO

        return new ExecuteStatementResp(StatusUtil.success(), false);

      case BEGIN_TRANSACTION:
        System.out.println("BEGIN_TRANSACTION");
        System.out.println("[DEBUG] " + plan);

        // TODO

        return new ExecuteStatementResp(StatusUtil.success(), false);

      case COMMIT:
        System.out.println("COMMIT");
        System.out.println("[DEBUG] " + plan);

        // TODO

        return new ExecuteStatementResp(StatusUtil.success(), false);

      case INSERT:
        System.out.println("INSERT");
        System.out.println("[DEBUG] " + plan);

        InsertPlan insertPlan = (InsertPlan) plan;
        String tableName = insertPlan.getTableName();
        Database dbForInsert = manager.getCurDB();
        Table tableToInsert = dbForInsert.getTable(tableName);
        MetaInfo metaInfo = dbForInsert.metaInfos.get(tableName);

        List<String> columnNames = insertPlan.getColumnNames();
        List<String> entryValues = insertPlan.getEntryValues();
        int columnNamesSize = columnNames.size(); // need check if 0
        int entryValuesSize = entryValues.size();

        ArrayList<Column> columns = tableToInsert.columns; // columns
        if (entryValuesSize != columns.size()) { // entry value size doesn't match column size
          return new ExecuteStatementResp(StatusUtil.fail("Input entry match failed."), false);
        }
        ArrayList<Entry> entries = new ArrayList<>(Collections.nCopies(columns.size(),null));

        if (columnNamesSize == 0) { // in order of original column order
          int current_entry_index = 0;
          for (Column c : columns) {
            if (c.getType() == STRING) {
              String cur_string_value = entryValues.get(current_entry_index);
              String new_string_value = cur_string_value.substring(1, cur_string_value.length() - 1);
              entryValues.set(current_entry_index, new_string_value);
            }
            entries.set(current_entry_index,
                    new Entry(Table.getColumnTypeValue(c.getType(), entryValues.get(current_entry_index))));
            current_entry_index += 1;
          }
        }
        else { // may not be in order of original column order
          // current_entry_index: index for entryValues
          for (int current_entry_index = 0; current_entry_index < entryValues.size(); ++current_entry_index) {
            int column_index = metaInfo.columnFind(columnNames.get(current_entry_index));
            ColumnType current_Type = columns.get(column_index).getType(); // current_entry column type

            if (current_Type == STRING) { // delete quote
              String cur_string_value = entryValues.get(current_entry_index);
              String new_string_value = cur_string_value.substring(1, cur_string_value.length() - 1);
              entryValues.set(current_entry_index, new_string_value);
            }
            entries.set(column_index,
                    new Entry(Table.getColumnTypeValue(
                            current_Type, entryValues.get(current_entry_index))));
          }
        }
        Row rowToInsert = new Row(entries);
        dbForInsert.getTable(tableName).insert(rowToInsert); // TODO: check


        return new ExecuteStatementResp(StatusUtil.success(), false);

      case DELETE:
        System.out.println("DELETE");
        System.out.println("[DEBUG] " + plan);
        return new ExecuteStatementResp(StatusUtil.success(), false);

      case UPDATE:
        System.out.println("UPDATE");
        System.out.println("[DEBUG] " + plan);
        return new ExecuteStatementResp(StatusUtil.success(), false);

      default:
    }
    return null;
  }
}
