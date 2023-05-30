package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.schema.Column;

import java.util.List;

public class InsertPlan extends LogicalPlan {

    private String tableName;
    private List<String> columnNames;
    private List<String> entryValues;

    public InsertPlan(String tableName, List<String> columnList, List<String> entryList) {
        super(LogicalPlanType.INSERT);
        this.tableName = tableName;
        this.columnNames = columnList;
        this.entryValues = entryList;
    }

    public String getTableName() {
        return tableName;
    }
    public List<String> getColumnNames() { return columnNames; }
    public List<String> getEntryValues() { return entryValues; }

    @Override
    public String toString() {
        return "InsertPlan{" + "tableName='" + tableName + '\'' + '}';
    }
    // TODO: FIX?
}
