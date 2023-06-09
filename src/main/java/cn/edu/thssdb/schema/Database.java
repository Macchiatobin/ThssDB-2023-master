package cn.edu.thssdb.schema;

import cn.edu.thssdb.exception.AlreadyExistsException;
import cn.edu.thssdb.exception.CustomIOException;
import cn.edu.thssdb.exception.FileException;
import cn.edu.thssdb.exception.NotExistsException;
import cn.edu.thssdb.parser.MySQLParser;
import cn.edu.thssdb.plan.LogicalPlan;
import cn.edu.thssdb.query.*;
import cn.edu.thssdb.transaction.MainTransaction;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static cn.edu.thssdb.utils.FolderOperations.deleteFolder;
import static cn.edu.thssdb.utils.Global.DATA_DIR;

public class Database implements Serializable {

  private String name;
  private transient HashMap<String, Table> tables;
  public HashMap<String, MetaInfo> metaInfos;
  transient ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private String path;
  private String metaPath;
  private MainTransaction transactionManager; // 事务管理
  private Logger logger; // 日志管理
  private List<Column> columns;

  public Database(String name) {
    this.name = name;
    this.tables = new HashMap<>();
    this.metaInfos = new HashMap<>();
    this.path = DATA_DIR + name + "/";
    this.metaPath = this.path + "meta";
    String folder = Paths.get("data", name).toString();
    String logger_name = name + ".log";
    this.logger = new Logger(folder, logger_name);
    recover();
  }

  public Logger getLogger() {
    return logger;
  }

  public MainTransaction getTransactionManager() {
    return transactionManager;
  }

  public Table getTable(String tableName) {
    System.out.println("Database getTable:" + tables.get(tableName));
    //    List<Column> cList = columns;
    //    Column column0 = new Column("column0", ColumnType.INT, 1, false, 0);
    //    Column column1 = new Column("column1", ColumnType.LONG, 0, false, 0);
    //    Column column2 = new Column("column2", ColumnType.FLOAT, 0, false, 0);
    //    Column column3 = new Column("column3", ColumnType.DOUBLE, 0, false, 0);
    //    Column column4 = new Column("column4", ColumnType.STRING, 0, false, 5);
    //    columns.add(column0);
    //    columns.add(column1);
    //    columns.add(column2);
    //    columns.add(column3);
    //    columns.add(column4);
    //    if(tables.get(tableName)==null)
    //    {
    //      Manager.getInstance().getCurDB().create(tableName,cList.toArray(new
    // Column[cList.size()]));
    //    }
    return tables.get(tableName);
  }

  public String getName() {
    return name;
  }

  private void persist() {
    // save as file, when changes made
    File meta_file = new File(this.metaPath);
    if (!meta_file.exists()) { // create meta file if not exists -> USUALLY NOT HAPPEN
      try {
        meta_file.createNewFile();
      } catch (Exception e) {
        throw new FileException(FileException.Create, meta_file.getName());
      }
    }

    lock.writeLock().lock();
    try (FileOutputStream fileOut = new FileOutputStream(this.metaPath);
        ObjectOutputStream objectOut = new ObjectOutputStream(fileOut)) {
      objectOut.writeObject(this); // Serialize database object
    } catch (IOException e) {
      System.out.println("Database Metafile Serialization Failed!");
      System.out.println(e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  // Create New Table
  public void create(String tableName, Column[] columns) {
    if (tables.get(tableName) != null) // table exists already
    {
      throw new AlreadyExistsException(AlreadyExistsException.Table, tableName);
    }

    lock.writeLock().lock();
    try {
      tables.put(tableName, new Table(this.name, tableName, columns, true));
      // set folder path for node Manager
      metaInfos.put(tableName, new MetaInfo(tableName, new ArrayList<>(Arrays.asList(columns))));
      persist();
    } catch (Exception e) {
      System.out.println(e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  // Drop Table
  public void drop(String tableName) { // drop table and delete files
    Table tobj;
    MetaInfo mobj;
    lock.writeLock().lock();
    try {
      mobj = metaInfos.get(tableName);
      tobj = tables.get(tableName);
      if (tobj == null) // table doesn't exist
      {
        throw new NotExistsException(NotExistsException.Table, tableName);
      }

      metaInfos.remove(tableName);
      tables.remove(tableName); // remove from HashMap if exists in it
      mobj = null;
      tobj = null;

      // delete corresponding file
      deleteFolder(new File(this.path + tableName));
      persist();
    } catch (Exception e) {
      tobj = null;
      mobj = null;
      System.out.println(e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  // originally return String
  public QueryResult select(
      QueryTable table, String[] columns, MultipleCondition mult_con, boolean isDistinct) {
    try {
      lock.readLock().lock();
      table.setMultipleCondition(mult_con); // may be null
      QueryResult query_res = new QueryResult(table, isDistinct, columns);
      query_res.obtainResults(); // join的问题所在?
      System.out.println("Database.select(): obtainResults() done"); // debug
      //      if (query_res == null) // 按理来说不该是null
      //      throw new QueryResultException();
      return query_res;
    } finally {
      lock.readLock().unlock();
    }
  }

  private void recover() { // read from file, when create
    transactionManager = new MainTransaction(name, getLogger());
    File dbFolder = new File(this.path);
    if (!dbFolder.exists()) // Create Folder, if first create
    {
      lock.writeLock().lock();
      try {
        boolean created = dbFolder.mkdir();
        if (!created) throw new IOException();
      } catch (IOException e) {
        System.out.println("Database File Creation Failed!");
      } finally {
        lock.writeLock().unlock();
      }
    }

    File metaFile = new File(this.metaPath);
    if (metaFile.exists()) {
      lock.writeLock().lock();
      try (FileInputStream fileInputStream = new FileInputStream(this.metaPath);
          ObjectInputStream inputStream = new ObjectInputStream(fileInputStream); ) {
        Database restored = (Database) inputStream.readObject(); // read from file

        // recover
        if (restored != null) {
          this.name = restored.name;
          this.metaInfos = restored.metaInfos;

          // recover tables manually
          this.tables = new HashMap<>();
          for (MetaInfo info : this.metaInfos.values()) {
            List<Column> columnList = info.getColumns();
            Column[] array = columnList.toArray(new Column[0]);
            tables.put(
                info.getTableName(), new Table(this.name, info.getTableName(), array, false));
          }
        }
      } catch (EOFException e) { // when some database got no table in it, no error!
        System.out.println("Empty database:" + this.name + ", this is no error!");
      } catch (IOException e) {
        System.out.println("InputStream Error Occurred During Recovering Database object!");
        System.out.println(e);
      } catch (ClassNotFoundException e) {
        System.out.println("ClassNotFoundError During Recovering Database object!");
        System.out.println(e);
      } finally {
        lock.writeLock().unlock();
      }
    } else { // create meta file if it doesn't exist
      try {
        metaFile.createNewFile();
      } catch (Exception e) {
        throw new FileException(FileException.Create, metaFile.getName());
      }
    }
    logRecover();
  }

  public void logRecover() {
    try {
      ArrayList<String> logs = this.logger.readLog();
      for (String log : logs) {
        //        String[] info = log.split(" ");
        //        String type = info[0];
        //        if (type.equals("DELETE")) {
        //          tables.get(info[1]).delete(info[2]);
        //        } else if (type.equals("INSERT")) {
        //          tables.get(info[1]).insert(info[2]);
        //        } else if (!type.equals("COMMIT")) {
        ArrayList<LogicalPlan> plans = MySQLParser.getOperations(log);
        for (LogicalPlan plan : plans) {
          try {
            plan.execute_plan();
          } catch (Exception e) {

          }
        }
      }
      //      }
    } catch (Exception e) {
      throw new CustomIOException();
    }
  }

  public void quit() {
    lock.writeLock().lock();
    try {
      persist();
      // TODO: wtf does this quit mean
    } catch (Exception e) {
      System.out.println(e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  // Build a single query table
  public QueryTable createSingleQueryTable(String table_name) {
    try {
      lock.readLock().lock();
      if (tables.containsKey(table_name)) {
        return new SingleQueryTable(tables.get(table_name));
      }
    } finally {
      lock.readLock().unlock();
    }
    throw new NotExistsException(NotExistsException.Table, table_name);
  }

  // Build a joined query table from multiple tables
  public QueryTable createJoinedQueryTable(
      ArrayList<String> table_names, MultipleCondition multiple_condition) {
    ArrayList<Table> joined_tables = new ArrayList<>();
    try {
      lock.readLock().lock();
      for (String table_name : table_names) {
        if (!this.tables.containsKey(table_name))
          throw new NotExistsException(NotExistsException.Table, table_name);
        joined_tables.add(this.tables.get(table_name));
      }
    } finally {
      lock.readLock().unlock();
    }
    return new MultipleQueryTable(joined_tables, multiple_condition);
  }
}
