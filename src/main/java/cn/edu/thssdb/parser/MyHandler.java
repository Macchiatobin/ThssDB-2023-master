package cn.edu.thssdb.parser;

import cn.edu.thssdb.query.QueryResult;
import cn.edu.thssdb.schema.Manager;
import java.util.ArrayList;
import java.util.Arrays;

public class MyHandler {
  private Manager manager;
  private String[] wal_cmds = {"insert", "delete", "update", "begin", "commit"};

  public MyHandler(Manager manager) {
    this.manager = manager;
  }

  public ArrayList<QueryResult> evaluate(String statement, long session) {
    System.out.println("session:" + session + "  " + statement);
    String cmd = statement.split("\\s+")[0];
    if (Arrays.asList(wal_cmds).contains(cmd.toLowerCase()) && session == 0) {
      manager.writelog(statement);
    }
  }
}
