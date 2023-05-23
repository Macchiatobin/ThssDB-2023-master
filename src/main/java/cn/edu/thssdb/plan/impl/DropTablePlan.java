package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;

public class DropTablePlan extends LogicalPlan {

    private String tableName;

    public DropTablePlan(String tableName) {
        super(LogicalPlanType.DROP_TABLE);
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }

    @Override
    public String toString() {
        return "DropTablePlan{" + "tableName='" + tableName + '\'' + '}';
    }
}