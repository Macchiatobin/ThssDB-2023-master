package cn.edu.thssdb.schema;

import cn.edu.thssdb.type.ColumnType;

import java.io.Serializable;

public class Column implements Comparable<Column>, Serializable {
  private String name;
  private ColumnType type;
  private int primary;
  private boolean notNull;
  private int maxLength;

  public Column(String name, ColumnType type, int primary, boolean notNull, int maxLength) {
    this.name = name;
    this.type = type;
    this.primary = primary;
    this.notNull = notNull;
    this.maxLength = maxLength;
  }

  public String getName() {
    return name;
  }

  public void setPrimary(int primary) {
    this.primary = primary;
  }

  @Override
  public int compareTo(Column e) {
    return name.compareTo(e.name);
  }

  public String toString() {
    return name + ',' + type + ',' + primary + ',' + notNull + ',' + maxLength;
  }

  public int getPrimary() {
    return primary;
  }

  public ColumnType getType() {
    return type;
  }

  // Added by Amy - 为ShowTable添加接口
  public boolean isNotNull() {
    return notNull;
  }

  public String getTypeString() {
    if (type == ColumnType.STRING) return "STRING";
    else if (type == ColumnType.LONG) return "LONG";
    else if (type == ColumnType.INT) return "INT";
    else if (type == ColumnType.FLOAT) return "FLOAT";
    else if (type == ColumnType.DOUBLE) return "DOUBLE";

    return "UNKNOWN_TYPE";
  }

  public int getMaxLength() {
    return maxLength;
  }
}
