package cn.edu.thssdb.schema;

import cn.edu.thssdb.exception.FileException;
import cn.edu.thssdb.exception.IllegalTypeException;
import cn.edu.thssdb.index.BPlusTree;
import cn.edu.thssdb.index.TreeNodeManager;
import cn.edu.thssdb.type.ColumnType;
import cn.edu.thssdb.utils.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static cn.edu.thssdb.type.ColumnType.*;
import static cn.edu.thssdb.utils.Global.DATA_DIR;

public class Table implements Iterable<Row>, Serializable {
  public transient ReentrantReadWriteLock lock;
  private String databaseName;
  public String tableName;
  public ArrayList<Column> columns;
  public BPlusTree<Entry, Row> index;
  private int primaryIndex; // index of primary key column
  private String path; // data file path
  private String metaPath;

  // Transaction Lock
  //  int lockPriority = 0;
  //  public ArrayList<Long> readLockList;
  //  public ArrayList<Long> writeLockList;

  public Table(String databaseName, String tableName, Column[] columns, boolean isFirst) {
    this.lock = new ReentrantReadWriteLock();
    this.databaseName = databaseName;
    this.tableName = tableName;
    this.columns = new ArrayList<>(Arrays.asList(columns));
    this.primaryIndex = -1;
    this.path = DATA_DIR + databaseName + "/" + tableName;
    this.metaPath = this.path + "/meta";
    //    this.lockPriority = 0;
    //    this.readLockList = new ArrayList<>();
    //    this.writeLockList = new ArrayList<>();

    for (int i = 0; i < this.columns.size(); i++) {
      if (this.columns.get(i).getPrimary() == 1) {
        primaryIndex = i;
      }
    }

    File tableFolder = new File(this.path);
    if (!tableFolder.exists()) tableFolder.mkdir(); // create folder if it doesn't exist

    if (!isFirst) {
      deserialize(); // recover index and call index.recover
    } else {
      this.index = new BPlusTree<>();
      this.index.nodeManager = new TreeNodeManager<>(index.root, path);
      serialize();
    }
  }

  public int getPrimaryIndex() {
    return primaryIndex;
  }

  // GET Row (Used when update)
  public Row get(Entry entry) {
    Row result = null;
    lock.readLock().lock();
    try {
      result = index.get(entry);
    } catch (Exception e) { // key not exists
      return null;
    } finally {
      lock.readLock().unlock();
    }
    return result;
  }

  // INSERT Row
  public void insert(Row row) {
    lock.writeLock().lock();
    try {
      Entry key = row.getEntries().get(primaryIndex);
      index.put(key, row);
      serialize();
    } catch (Exception e) {
      throw e;
    } finally {
      lock.writeLock().unlock();
    }
  }

  // getType
  public static Comparable getColumnTypeValue(ColumnType c, String val) {
    if (val.equalsIgnoreCase("null")) return null;
    if (c == INT) return Double.valueOf(val).intValue();
    else if (c == LONG) return Double.valueOf(val).longValue();
    else if (c == FLOAT) return Double.valueOf(val).floatValue();
    else if (c == DOUBLE) return Double.valueOf(val);
    else if (c == STRING) return val;
    else throw new IllegalTypeException();
  }

  // INSERT String
  public void insert(String val) {
    // TODO
    lock.writeLock().lock();
    try {
      String[] tmp = val.split(",");
      ArrayList<Entry> entries = new ArrayList<>();
      int i = 0;
      for (Column c : columns) {
        entries.add(new Entry(getColumnTypeValue(c.getType(), tmp[i])));
        i++;
      }
      index.put(entries.get(primaryIndex), new Row(entries));
      serialize();
    } catch (Exception e) {
      System.out.println(e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  // DELETE Row
  public void delete(Row row) {
    // TODO
    lock.writeLock().lock();
    try {
      Entry key = row.getEntries().get(primaryIndex);
      index.remove(key);
      serialize();
    } catch (Exception e) {
      System.out.println(e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  // DELETE Entry
  public void delete(Entry entry) {
    // TODO
    lock.writeLock().lock();
    try {
      index.remove(entry);
      serialize();
    } catch (Exception e) {
      System.out.println(e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  // DELETE String
  public void delete(String val) {
    lock.writeLock().lock();
    try {
      ColumnType columnType = columns.get(primaryIndex).getType();
      Entry primaryEntry = new Entry(getColumnTypeValue(columnType, val));
      index.remove(primaryEntry);
      serialize();
    } catch (Exception e) {
      System.out.println(e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  // DELETE All Row
  public void delete() {
    lock.writeLock().lock();
    try {
      index.clear();
      index = new BPlusTree<>();
      serialize();
    } catch (Exception e) {
      System.out.println(e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void update(Row oldRow, Row newRow) { // simple implementation
    lock.writeLock().lock();
    try {
      delete(oldRow);
      insert(newRow);
      serialize();
    } catch (Exception e) {
      System.out.println(e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  private void serialize() { // persist
    // TODO
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
    try (ObjectOutputStream objectOutputStream =
        new ObjectOutputStream(new FileOutputStream(meta_file)); ) {
      objectOutputStream.writeObject(this);
    } catch (Exception e) {
      System.out.println(e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  private void deserialize() { // recover
    File meta_file = new File(this.metaPath);
    if (!meta_file.exists()) return; // no file, nothing to recover

    lock.writeLock().lock();
    try (ObjectInputStream objectInputStream =
        new ObjectInputStream(new FileInputStream(meta_file)); ) {
      Table restored = (Table) objectInputStream.readObject();

      if (restored != null) {
        this.index = restored.index;
        index.recover(this.path);
      }
    } catch (Exception e) {
      System.out.println(e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  // Transaction Lock
  //  public int acquireReadLock(long session) {
  //    int checkFlag = 0; // 返回-1代表加锁失败 返回0代表成功但未加锁 返回1代表成功加锁
  //
  //    if (lockPriority == 2) {
  //      if (writeLockList.contains(session)) { // 自身已经有更高级的锁了 用x锁去读，未加锁
  //        checkFlag = 0;
  //      } else {
  //        checkFlag = -1; // 别的session占用x锁，未加锁
  //      }
  //    } else if (lockPriority == 1) {
  //      if (readLockList.contains(session)) { // 自身已经有s锁了 用s锁去读，未加锁
  //        checkFlag = 0;
  //      } else {
  //        readLockList.add(session); // 其他session加了s锁 把自己加上
  //        lockPriority = 1;
  //        checkFlag = 1;
  //      }
  //    } else if (lockPriority == 0) {
  //      readLockList.add(session); // 未加锁 把自己加上
  //      lockPriority = 1;
  //      checkFlag = 1;
  //    }
  //
  //    return checkFlag;
  //  }
  //
  //  public void releaseReadLock(long session) {
  //    if (readLockList.contains(session)) {
  //      readLockList.remove(session);
  //
  //      if (readLockList.isEmpty()) {
  //        lockPriority = 0;
  //      } else {
  //        lockPriority = 1;
  //      }
  //    }
  //  }
  //
  //  public int acquireWriteLock(long session) {
  //    int checkFlag = 0; // 返回-1代表加锁失败 返回0代表成功但未加锁 返回1代表成功加锁
  //
  //    if (lockPriority == 2) {
  //      if (writeLockList.contains(session)) { // 自身已经取得x锁
  //        checkFlag = 0;
  //      } else {
  //        checkFlag = -1; // 获取x锁失败
  //      }
  //    } else if (lockPriority == 1) {
  //      checkFlag = -1; // 正在被其他s锁占用
  //    } else if (lockPriority == 0) {
  //      writeLockList.add(session);
  //      lockPriority = 2;
  //      checkFlag = 1;
  //    }
  //
  //    return checkFlag;
  //  }
  //
  //  public void releaseWriteLock(long session) {
  //    if (writeLockList.contains(session)) {
  //      lockPriority = 0;
  //      writeLockList.remove(session);
  //    }
  //  }

  // TODO: use?
  private class TableIterator implements Iterator<Row> {
    private Iterator<Pair<Entry, Row>> iterator;

    TableIterator(Table table) {
      this.iterator = table.index.iterator();
    }

    @Override
    public boolean hasNext() {
      return iterator.hasNext();
    }

    @Override
    public Row next() {
      return iterator.next().right;
    }
  }

  @Override
  public Iterator<Row> iterator() {
    return new TableIterator(this);
  }
}
