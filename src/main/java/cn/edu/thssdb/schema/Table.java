package cn.edu.thssdb.schema;

import cn.edu.thssdb.exception.IllegalTypeException;
import cn.edu.thssdb.index.BPlusTree;
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
  transient ReentrantReadWriteLock lock;
  private String databaseName;
  public String tableName;
  public ArrayList<Column> columns;
  public BPlusTree<Entry, Row> index;
  private int primaryIndex; // index of primary key column
  private String path; // data file path

  public Table(String databaseName, String tableName, Column[] columns) {
    // TODO
    this.lock = new ReentrantReadWriteLock();
    this.databaseName = databaseName;
    this.tableName = tableName;
    this.columns = new ArrayList<>(Arrays.asList(columns));
    this.index = new BPlusTree<>();
    this.primaryIndex = -1;
    this.path = DATA_DIR + databaseName + "/" + tableName;
    for (int i = 0; i < this.columns.size(); i++) {
      if (this.columns.get(i).get_Primary() == 1) {
        primaryIndex = i;
      }
    }
    File tableFolder = new File(this.path);
    if (!tableFolder.exists()) tableFolder.mkdir(); // create folder if it doesn't exists
  }

  // maybe no use
  private void recover() {
  }



  // INSERT Row
  public void insert(Row row) {
    try {
      lock.writeLock().lock();
      Entry key = row.getEntries().get(primaryIndex);
      index.put(key, row);
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
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void update(Row oldRow, Row newRow) {
    // TODO
    Entry oldIndex = oldRow.getEntries().get(primaryIndex);
    Entry newIndex = newRow.getEntries().get(primaryIndex);
    lock.writeLock().lock();
    try {
      if (oldIndex.compareTo(newIndex) == 0) {
        index.update(newIndex, newRow);
      } else {
        delete(oldRow);
        insert(newRow);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  private void serialize() throws IOException { // persist
    // TODO
    lock.writeLock().lock();
    try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(path));)
    {
      objectOutputStream.writeObject(this);
    } finally {
      lock.writeLock().unlock();
    }
  }

  private void deserialize() { // recover
    lock.writeLock().lock();
    try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(path));) {
      Table restored = (Table) objectInputStream.readObject();

      // TODO: restore only nodes that we need for now

      if (restored != null) {
        this.columns = restored.columns;
        this.index = restored.index;
        this.primaryIndex = restored.primaryIndex;
        this.path = DATA_DIR + databaseName + "/" + tableName;
      }
    } catch (Exception e) {
      System.out.println(e);
    } finally {
      lock.writeLock().unlock();
    }
  }

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
