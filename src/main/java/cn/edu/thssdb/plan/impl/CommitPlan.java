package cn.edu.thssdb.plan.impl;

import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.rpc.thrift.ExecuteStatementResp;

public class CommitPlan extends LogicalPlan {

    public CommitPlan() {
        super(LogicalPlanType.COMMIT);
    }

    @Override
    public String toString() {
        return "CommitPlan";
    }

    @Override
    public ExecuteStatementResp execute_plan() {
        return null;
    }
}
