package cn.edu.thssdb.schema;

import cn.edu.thssdb.exception.AlreadyExistsException;
import cn.edu.thssdb.exception.FileException;
import cn.edu.thssdb.exception.NotExistsException;

import java.io.*;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static cn.edu.thssdb.utils.FolderOperations.deleteFolder;
import static cn.edu.thssdb.utils.Global.DATA_DIR;

public class Manager implements Serializable {
  private HashMap<String, Database> databases;
  private Database curDB;
  private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private String metaPath = DATA_DIR + "manager_meta/";

  // Transaction Lock
  //  public ArrayList<Long> transaction_sessions; // 处于transaction状态的session列表
  //  public ArrayList<Long> lockTransactionList; // 由于锁阻塞的session队列
  //  public HashMap<Long, ArrayList<String>> readLockMap; // 记录每个session取得了哪些表的s锁
  //  public HashMap<Long, ArrayList<String>> writeLockMap; // 记录每个session取得了哪些表的x锁

  public static Manager getInstance() {
    return Manager.ManagerHolder.INSTANCE;
  }

  private Manager() {
    databases = new HashMap<>();
    curDB = null;
    loadData(); // recover

    //    readLockMap = new HashMap<>();
    //    writeLockMap = new HashMap<>();
    //    transaction_sessions = new ArrayList<>();
    //    lockTransactionList = new ArrayList<>();
  }

  public Database getCurDB() {
    return curDB;
  }

  public void createDatabaseIfNotExists(String databaseName) {
    /* TODO */
    // v1 done
    //    lock.writeLock().lock();  //  为啥要挪出来？
    try {
      lock.writeLock().lock(); // original
      if (databases.get(databaseName) != null) // exists already
      {
        throw new AlreadyExistsException(AlreadyExistsException.Database, databaseName);
      }
      Database newDB = new Database(databaseName);
      databases.put(databaseName, newDB);
      persist();
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void deleteDatabase(String databaseName) {
    // v1 done
    lock.writeLock().lock();
    try {
      if (databases.get(databaseName) == null)
        throw new NotExistsException(NotExistsException.Database, databaseName);
      databases.remove(databaseName);
      String folderPath = DATA_DIR + databaseName;
      deleteFolder(new File(folderPath));
      persist();
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void switchDatabase(String databaseName) {
    /* TODO */
    // v1 done
    lock.readLock().lock();
    try {
      if (!databases.containsKey(databaseName))
        throw new NotExistsException(NotExistsException.Database, databaseName);
      curDB = getDB(databaseName);
    } finally {
      lock.readLock().unlock();
    }
  }

  // 单例模式
  private static class ManagerHolder implements Serializable {
    private static final Manager INSTANCE = new Manager();

    private ManagerHolder() {}
  }

  private void loadData() {
    File data_dir = new File(DATA_DIR);
    if (!data_dir.exists()) data_dir.mkdir(); // create directory if not exists
    File data_file = new File(metaPath);
    if (!data_file.exists()) { // create file if not exists
      try {
        data_file.createNewFile();
      } catch (Exception e) {
        throw new FileException(FileException.Create, data_file.getName());
      }
    } else { // read from existing file
      try {
        BufferedReader reader = new BufferedReader(new FileReader(data_file));
        String cur_line = null;
        while ((cur_line = reader.readLine()) != null) { // cur_line is databaseName
          databases.put(cur_line, new Database(cur_line)); // load databases
          //          readLog(cur_line);
        }
        reader.close();
      } catch (Exception e) {
        throw new FileException(FileException.ReadWrite, data_file.getName());
      }
    }
  }

  public void persist() { // only persists names of databases
    File data_file = new File(metaPath);
    if (!data_file.exists()) { // create file if not exists
      try {
        data_file.createNewFile();
      } catch (Exception e) {
        throw new FileException(FileException.Create, data_file.getName());
      }
    }

    // write to file (from beginning), only database names
    try {
      FileWriter writer = new FileWriter(metaPath);
      for (String databaseName : databases.keySet()) {
        writer.write(databaseName);
        writer.write("\n");
      }
      writer.close();
    } catch (Exception e) {
      throw new FileException(FileException.ReadWrite, metaPath);
    }
  }

  private Database getDB(String databaseName) {
    //    lock.readLock().lock(); // 为啥要挪出来？
    try {
      lock.readLock().lock(); // original
      if (!databases.containsKey(databaseName))
        throw new NotExistsException(NotExistsException.Database, databaseName);
      return databases.get(databaseName);
    } finally {
      lock.readLock().unlock();
    }
  }

  // 恢复机制 Log
//    public void writeLog(String statement) {
//      Database current_base = getCurDB();
//      String database_name = current_base.getName();
//      String filename = DATA_DIR + database_name + ".log";
//      System.out.println("Log File: " + filename);
//      try {
//        FileWriter writer = new FileWriter(filename, true);
//        System.out.println("Log File Create Success: " + filename);
//        System.out.println(statement);
//        writer.write(statement + "\n");
//        writer.close();
//      } catch (IOException e) {
//        e.printStackTrace();
//      }
//    }
  //
  //  public void readLog(String database_name) {
  //
  //    String log_name = "data/" + database_name + ".log";
  //    File file = new File(log_name);
  //    if (file.exists() && file.isFile()) {
  //      System.out.println("log file size: " + file.length() + " Byte");
  //      System.out.println("Read WAL log to recover database.");
  //      evaluate("use " + database_name);
  //
  //      try {
  //        InputStreamReader reader = new InputStreamReader(Files.newInputStream(file.toPath()));
  //        BufferedReader bufferedReader = new BufferedReader(reader);
  //        String line;
  //        ArrayList<String> lines = new ArrayList<>();
  //        ArrayList<Integer> transactionList = new ArrayList<>();
  //        ArrayList<Integer> commit_list = new ArrayList<>();
  //        int index = 0;
  //        while ((line = bufferedReader.readLine()) != null) {
  //          if (line.equals("begin transaction")) {
  //            transactionList.add(index);
  //          } else if (line.equals("commit")) {
  //            commit_list.add(index);
  //          }
  //          lines.add(line);
  //          index++;
  //        }
  //        int last_cmd = 0;
  //        if (transactionList.size() == commit_list.size()) {
  //          last_cmd = lines.size() - 1;
  //        } else {
  //          last_cmd = transactionList.get(transactionList.size() - 1) - 1;
  //        }
  //        for (int i = 0; i <= last_cmd; i++) {
  //          evaluate(lines.get(i));
  //        }
  //        System.out.println("read " + (last_cmd + 1) + " lines");
  //        reader.close();
  //        bufferedReader.close();
  //
  //        // 清空log并重写实际执行部分
  //        if (transactionList.size() != commit_list.size()) {
  //          FileWriter writer1 = new FileWriter(log_name);
  //          writer1.write("");
  //          writer1.close();
  //          FileWriter writer2 = new FileWriter(log_name, true);
  //          for (int i = 0; i <= last_cmd; i++) {
  //            writer2.write(lines.get(i) + "\n");
  //          }
  //          writer2.close();
  //        }
  //
  //      } catch (IOException e) {
  //        e.printStackTrace();
  //      }
  //    }
  //  }

  //  public String evaluate(String statement) {
  //    // 处理词法
  //    SQLLexer lexer = new SQLLexer(CharStreams.fromString(statement));
  //    lexer.removeErrorListeners();
  //    lexer.addErrorListener(SQLParseError.INSTANCE);
  //    CommonTokenStream tokens = new CommonTokenStream(lexer);
  //
  //    // 处理句法
  //    SQLParser parser = new SQLParser(tokens);
  //    parser.removeErrorListeners();
  //    parser.addErrorListener(SQLParseError.INSTANCE);
  //
  //    // 语义处理
  //    try {
  //      ThssDBSQLVisitor visitor = new ThssDBSQLVisitor(getInstance());
  //      return String.valueOf(visitor.visitParse(parser.parse()));
  //    } catch (Exception e) {
  //      throw new IllegalSQLStatementException(e.getMessage());
  //    }
  //  }
}
