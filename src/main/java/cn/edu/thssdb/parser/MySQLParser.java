package cn.edu.thssdb.parser;

import cn.edu.thssdb.exception.IllegalSQLStatementException;
import cn.edu.thssdb.plan.LogicalPlan;
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
  public static ArrayList<LogicalPlan> getOperations(String statement) {
    SQLLexer lexer = new SQLLexer(CharStreams.fromString(statement));
    lexer.removeErrorListeners();
    lexer.addErrorListener(MyErrorListener.INSTANCE);
    CommonTokenStream tokenStream = new CommonTokenStream(lexer);
    SQLParser parser = new SQLParser(tokenStream);
    parser.removeErrorListeners();
    parser.addErrorListener(MyErrorListener.INSTANCE);
    ArrayList res;
    try {
      ThssDBSQLVisitor visitor = new ThssDBSQLVisitor();
      LogicalPlan logicalPlan = (LogicalPlan) visitor.visit(parser.parse()); // Retrieve the LogicalPlan object
      res = new ArrayList<>(); // Create a new ArrayList
      res.add(logicalPlan); // Add the LogicalPlan object to the ArrayList
    } catch (Exception e) {
      throw e; // 这里抛出异常让上层处理
    }
    return res;
  }

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
