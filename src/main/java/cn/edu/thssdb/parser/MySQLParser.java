package cn.edu.thssdb.parser;

import cn.edu.thssdb.exception.IllegalSQLStatementException;
import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.query.QueryResult;
import cn.edu.thssdb.schema.Manager;
import cn.edu.thssdb.sql.SQLLexer;
import cn.edu.thssdb.sql.SQLParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.ArrayList;

/** 描述:处理sql的主类，需要传入manager */
public class MySQLParser {
  private Manager manager;
  //  private String[] wal_cmds = {"insert", "delete", "update", "begin", "commit"};

  public MySQLParser(Manager manager) {
    this.manager = manager;
  }

  // TODO: 和transaction交互

  // 不需session id的版本
  public String process(String statement) {
    // 处理词法
    SQLLexer lexer = new SQLLexer(CharStreams.fromString(statement));
    lexer.removeErrorListeners();
    lexer.addErrorListener(SQLParseError.INSTANCE);
    CommonTokenStream tokens = new CommonTokenStream(lexer);

    // 处理句法
    SQLParser parser = new SQLParser(tokens);
    parser.removeErrorListeners();
    parser.addErrorListener(SQLParseError.INSTANCE);

    // 语义处理
    try {
      ThssDBSQLVisitor visitor = new ThssDBSQLVisitor();
      return String.valueOf(visitor.visitParse(parser.parse()));
    } catch (Exception e) {
      throw new IllegalSQLStatementException(e.getMessage());
    }
  }
}
