package cn.edu.thssdb.service;

import cn.edu.thssdb.plan.LogicalGenerator;
import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.plan.impl.*;
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
import cn.edu.thssdb.schema.Column;
import cn.edu.thssdb.schema.Database;
import cn.edu.thssdb.schema.Manager;
import cn.edu.thssdb.schema.Table;
import cn.edu.thssdb.utils.Global;
import cn.edu.thssdb.utils.StatusUtil;
import org.apache.thrift.TException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

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
        break;

      case DROP_DB:
        System.out.println("DROP_DB");
        System.out.println("[DEBUG] " + plan);
        break;

      case USE_DB:
        System.out.println("USE_DB");
        System.out.println("[DEBUG] " + plan);
        break;

      case CREATE_TABLE:
        System.out.println("CREATE_TABLE");
        System.out.println("[DEBUG] " + plan);
        break;

      case DROP_TABLE:
        System.out.println("DROP_TABLE");
        System.out.println("[DEBUG] " + plan);
        break;

      case QUIT:
        System.out.println("QUIT");
        System.out.println("[DEBUG] " + plan);

        // TODO: wtf does this quit mean

        break;

      case SHOW_TABLE:
        System.out.println("SHOW_TABLE");
        System.out.println("[DEBUG] " + plan);
        ExecuteStatementResp resp = new ExecuteStatementResp(StatusUtil.success(), true);
        String tableName = ((ShowTablePlan) plan).getTableName();
        Database curDB = manager.getCurDB();
        Table curTable = curDB.getTable(tableName);
        resp.addToColumnsList("Table \'" + tableName + "\'\n" + "ColumnName");
        resp.addToColumnsList("ColumnType");
        resp.addToColumnsList("IsPrimaryKey");
        resp.addToColumnsList("IsNotNull");
        resp.addToColumnsList("MaxLength");
        ArrayList<Column> columns = curTable.columns;
        for (Column column : columns) {
          ArrayList<String> curColumn = new ArrayList<>();
          curColumn.add(column.getName());
          curColumn.add(column.getTypeString());
          curColumn.add("" + column.getPrimary());
          curColumn.add(String.valueOf(column.isNotNull()));
          curColumn.add("" + column.getMaxLength());
          resp.addToRowList(curColumn);
        }
        return resp;

        // TODO: Plan, MetaInfo printing implementation

      case AUTO_COMMIT:
        System.out.println("AUTO_COMMIT");
        System.out.println("[DEBUG] " + plan);

        // TODO

        break;

      case BEGIN_TRANSACTION:
        System.out.println("BEGIN_TRANSACTION");
        System.out.println("[DEBUG] " + plan);

        // TODO

        break;

      case COMMIT:
        System.out.println("COMMIT");
        System.out.println("[DEBUG] " + plan);

        // TODO

        break;

      case INSERT:
        System.out.println("INSERT");
        System.out.println("[DEBUG] " + plan);
        break;

      case DELETE:
        System.out.println("DELETE");
        System.out.println("[DEBUG] " + plan);
        break;

      case UPDATE:
        System.out.println("UPDATE");
        System.out.println("[DEBUG] " + plan);
        break;

      default:break;
    }
    return plan.execute_plan();
  }
}
