package cn.edu.thssdb.service;

import cn.edu.thssdb.parser.MySQLParser;
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
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.edu.thssdb.utils.Global.DATA_DIR;

public class IServiceHandler implements IService.Iface {

  private Manager manager;
  private static final AtomicInteger sessionCnt = new AtomicInteger(0);
  public static MySQLParser handler;

  public IServiceHandler() {
    this.manager = Manager.getInstance();
    handler = new MySQLParser(manager);

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
    //    long the_session = req.getSessionId();
    //    ArrayList<QueryResult> the_result;
    //    ArrayList<QueryResult> result = new ArrayList<>();

    if (req.getSessionId() < 0) {
      return new ExecuteStatementResp(
          StatusUtil.fail("You are not connected. Please connect first."), false);
    }
    // TODO: implement execution logic，需要实现日志记录等
    // 事务管理
    LogicalPlan plan = LogicalGenerator.generate(manager, req.statement);
    System.out.println("Logical Plan!");

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
        LinkedList<String> log_create = new LinkedList<>();
        String command_full_create = req.statement;
        String[] commands_create = command_full_create.split(";");
        for (String command : commands_create) {
          command = command.trim();
          System.out.println("Command: " + command);
          log_create.add(command);
        }
        System.out.println("CREATE_TABLE");
        System.out.println("[DEBUG] " + plan);
        Manager.getInstance().getCurDB().getTransactionManager().exec(plan, log_create);
        break;

      case DROP_TABLE:
        LinkedList<String> log_drop = new LinkedList<>();
        String command_full_drop = req.statement;
        String[] commands_drop = command_full_drop.split(";");
        for (String command : commands_drop) {
          command = command.trim();
          System.out.println("Command: " + command);
          log_drop.add(command);
        }
        System.out.println("DROP_TABLE");
        System.out.println("[DEBUG] " + plan);
        Manager.getInstance().getCurDB().getTransactionManager().exec(plan, log_drop);
        break;

      case QUIT:
        System.out.println("QUIT");
        System.out.println("[DEBUG] " + plan);
        Manager.getInstance().getCurDB().getTransactionManager().exec(plan);
        break;

      case SHOW_TABLE:
        System.out.println("SHOW_TABLE");
        System.out.println("[DEBUG] " + plan);
        ExecuteStatementResp resp = new ExecuteStatementResp(StatusUtil.success(), true);
        String tableName = ((ShowTablePlan) plan).getSpecificTableName();
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
        //        transactionManager = new MainTransaction(manager.getCurDB().getName());
        System.out.println(plan);
        System.out.println("Transaction!");
        Manager.getInstance().getCurDB().getTransactionManager().exec(plan);
        System.out.println("Begin Exec");
        break; //        return plan.execute_plan();

        // TODO

      case COMMIT:
        System.out.println("COMMIT");
        System.out.println("[DEBUG] " + plan);
        //        transactionManager = new MainTransaction(manager.getCurDB().getName());
        System.out.println("Transaction!");
        Manager.getInstance().getCurDB().getTransactionManager().exec(plan);
        System.out.println("Commit Exec");
        break;
        //        return new ExecuteStatementResp(StatusUtil.success(), false);
        //                return plan.execute_plan();

        // TODO

      case INSERT:
        LinkedList<String> log_insert = new LinkedList<>();
        String command_full_insert = req.statement;
        String[] commands_insert = command_full_insert.split(";");
        for (String command : commands_insert) {
          command = command.trim();
          System.out.println("Command: " + command);
          log_insert.add(command);
        }

        System.out.println("INSERT");
        System.out.println("[DEBUG] " + plan);
        //        transactionManager = new MainTransaction(manager.getCurDB().getName());
        System.out.println("Transaction!");
        Manager.getInstance().getCurDB().getTransactionManager().exec(plan, log_insert);
        System.out.println("Insert Exec");
        break;
        //        return new ExecuteStatementResp(StatusUtil.success(), false);
        //        return plan.execute_plan();
        //        if (!manager.transaction_sessions.contains(the_session)) {
        //          System.out.println(the_session);
        //
        //          handler.evaluate("AUTO-BEGIN TRANSACTION", the_session);
        //          the_result = handler.evaluate("INSERT", the_session);
        //          result.addAll(the_result);
        //          handler.evaluate("AUTO COMMIT", the_session);
        //
        //        } else {
        //          the_result = handler.evaluate("INSERT", the_session);
        //          result.addAll(the_result);
        //        }

      case DELETE:
        LinkedList<String> log_delete = new LinkedList<>();
        String command_full_delete = req.statement;
        String[] commands_delete = command_full_delete.split(";");
        for (String command : commands_delete) {
          command = command.trim();
          System.out.println("Command: " + command);
          log_delete.add(command);
        }
        System.out.println("DELETE");
        System.out.println("[DEBUG] " + plan);
        //        transactionManager = new MainTransaction(manager.getCurDB().getName());
        System.out.println("Transaction!");
        Manager.getInstance().getCurDB().getTransactionManager().exec(plan, log_delete);
        System.out.println("Delete Exec");
        break;
        //        return new ExecuteStatementResp(StatusUtil.success(), false);
        //        return plan.execute_plan();

        //      if (!manager.transaction_sessions.contains(the_session)) {
        //          handler.evaluate("AUTO-BEGIN TRANSACTION", the_session);
        //          the_result = handler.evaluate("DELETE", the_session);
        //          result.addAll(the_result);
        //          handler.evaluate("AUTO COMMIT", the_session);
        //
        //        } else {
        //          the_result = handler.evaluate("DELETE", the_session);
        //          result.addAll(the_result);
        //        }

      case UPDATE:
        LinkedList<String> log_update = new LinkedList<>();
        String command_full_update = req.statement;
        String[] commands_update = command_full_update.split(";");
        for (String command : commands_update) {
          command = command.trim();
          System.out.println("Command: " + command);
          log_update.add(command);
        }
        System.out.println("UPDATE");
        System.out.println("[DEBUG] " + plan);
        //        transactionManager = new MainTransaction(manager.getCurDB().getName());
        System.out.println("Transaction!");
        Manager.getInstance().getCurDB().getTransactionManager().exec(plan, log_update);
        System.out.println("Update Exec");
        break;
        //        return new ExecuteStatementResp(StatusUtil.success(), false);
        //        return plan.execute_plan();

        //        if (!manager.transaction_sessions.contains(the_session)) {
        //          handler.evaluate("AUTO-BEGIN TRANSACTION", the_session);
        //          the_result = handler.evaluate("UPDATE", the_session);
        //          result.addAll(the_result);
        //          handler.evaluate("AUTO COMMIT", the_session);
        //
        //        } else {
        //          the_result = handler.evaluate("UPDATE", the_session);
        //          result.addAll(the_result);
        //        }

      case SELECT:
        /* TODO */
        System.out.println("SELECT");
        System.out.println("[DEBUG] " + plan);
        //        transactionManager = new MainTransaction(manager.getCurDB().getName());
        System.out.println("Transaction!");
        Manager.getInstance().getCurDB().getTransactionManager().exec(plan);
        System.out.println("Select Exec");
        break;
        //        return new ExecuteStatementResp(StatusUtil.success(), false);
        //        return plan.execute_plan();

        //        if (!manager.transaction_sessions.contains(the_session)) {
        //          handler.evaluate("AUTO-BEGIN TRANSACTION", the_session);
        //          the_result = handler.evaluate("SELECT", the_session);
        //          result.addAll(the_result);
        //          handler.evaluate("AUTO COMMIT", the_session);
        //
        //        } else {
        //          the_result = handler.evaluate("SELECT", the_session);
        //          result.addAll(the_result);
        //        }
        //        return ((SelectPlan)plan).execute_plan(req); // 加了个req参数，用于和transaction交互

      default:
        break;
    }
    return plan.execute_plan();
  }
}
