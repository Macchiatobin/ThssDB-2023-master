package cn.edu.thssdb.transaction;

import java.util.ArrayList;

public class TransactionFlag {
  public static class Table {
    public String title;
    public ArrayList<String> columns;
    public ArrayList<ArrayList<String>> data;

    public Table(ArrayList<ArrayList<String>> data, ArrayList<String> columns, String title) {
      this.title = title;
      this.columns = columns;
      this.data = data;
    }
  }

  private final boolean flag;
  private final String info;
  private final Table table;

  public TransactionFlag(boolean flag, String info) {
    this.flag = flag;
    this.info = info;
    this.table = null;
  }

  public boolean getFlag() {
    return flag;
  }

  public String getInfo() {
    return info;
  }

  public Table getTable() {
    return table;
  }
}
