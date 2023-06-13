package cn.edu.thssdb.parser;

import cn.edu.thssdb.exception.IllegalSQLStatementException;
import cn.edu.thssdb.query.QueryResult;
import cn.edu.thssdb.schema.Manager;
import cn.edu.thssdb.sql.SQLLexer;
import cn.edu.thssdb.sql.SQLParser;
import java.util.ArrayList;
import java.util.Arrays;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

/** 描述:处理sql的主类，需要传入manager */
public class MySQLParser {
  private Manager manager;
  private String[] wal_cmds = {"insert", "delete", "update", "begin", "commit"};

  public MySQLParser(Manager manager) {
    this.manager = manager;
  }

  // TODO: 和transaction交互
  public ArrayList<QueryResult> evaluate(String statement, long session) {
    System.out.println("session:" + session + "  " + statement);
    String cmd = statement.split("\\s+")[0];
    if (Arrays.asList(wal_cmds).contains(cmd.toLowerCase()) && session == 0) {
      manager.writeLog(statement);
    }
    // 词法分析
    SQLLexer lexer = new SQLLexer(CharStreams.fromString(statement));
    lexer.removeErrorListeners();
    lexer.addErrorListener(SQLParseError.INSTANCE);
    CommonTokenStream tokens = new CommonTokenStream(lexer);

    // 句法分析
    SQLParser parser = new SQLParser(tokens);
    parser.removeErrorListeners();
    parser.addErrorListener(SQLParseError.INSTANCE);
    // 语义分析
    try {
      ThssDBSQLVisitor visitor = new ThssDBSQLVisitor(manager);
      String message = "Transaction evaluate Result";
      QueryResult the_result = new QueryResult(message);
      ArrayList<QueryResult> result = new ArrayList<>();
      result.add(the_result);
      return result;
    } catch (Exception e) {
      String message = "Exception: illegal SQL statement! Error message: " + e.getMessage();
      QueryResult the_result = new QueryResult(message);
      ArrayList<QueryResult> result = new ArrayList<>();
      result.add(the_result);
      return result;
    }
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
      ThssDBSQLVisitor visitor = new ThssDBSQLVisitor(manager);
      return String.valueOf(visitor.visitParse(parser.parse()));
    } catch (Exception e) {
      throw new IllegalSQLStatementException(e.getMessage());
    }
  }
}
