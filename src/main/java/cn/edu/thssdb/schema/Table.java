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

public class Table implements Iterable<Row>, Serializable {
  ReentrantReadWriteLock lock;
  private String databaseName;
  public String tableName;
  public ArrayList<Column> columns; // Amy: 是否要改成私有变量 + 公有接口？
  public BPlusTree<Entry, Row> index;
  private int primaryIndex;
  public static final String DATA_DIRECTORY = "data/";

  public Table(String databaseName, String tableName, Column[] columns) {
    // TODO
    this.lock = new ReentrantReadWriteLock();
    this.databaseName = databaseName;
    this.tableName = tableName;
    this.columns = new ArrayList<>(Arrays.asList(columns));
    this.index = new BPlusTree<>();
    this.primaryIndex = -1;
    for (int i = 0; i < this.columns.size(); i++) {
      if (this.columns.get(i).getPrimary() == 1) {
        primaryIndex = i;
      }
    }
    recover();
  }

  private void recover() {
    // TODO
  }

  // INSERT Row
  public void insert(Row row) {
    // TODO
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
    try {
      lock.writeLock().lock();
      Entry key = row.getEntries().get(primaryIndex);
      index.remove(key);
    } finally {
      lock.writeLock().unlock();
    }
  }

  // DELETE Entry
  public void delete(Entry entry) {
    // TODO
    try {
      lock.writeLock().lock();
      index.remove(entry);
    } finally {
      lock.writeLock().unlock();
    }
  }

  // DELETE String
  public void delete(String val) {
    // TODO
    try {
      lock.writeLock().lock();
      ColumnType columnType = columns.get(primaryIndex).getType();
      Entry primaryEntry = new Entry(getColumnTypeValue(columnType, val));
      index.remove(primaryEntry);
    } finally {
      lock.writeLock().unlock();
    }
  }

  // DELETE All Row
  public void delete() {
    // TODO
    try {
      lock.writeLock().lock();
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
    try {
      lock.writeLock().lock();
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

  private void serialize(ArrayList<Row> rows, String filename) throws IOException {
    // TODO
    ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(filename));
    objectOutputStream.writeObject(rows);
    objectOutputStream.close();
  }

  private ArrayList<Row> deserialize(File file) {
    // TODO
    ArrayList<Row> rows;
    try {
      ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file));
      rows = (ArrayList<Row>) objectInputStream.readObject();
      objectInputStream.close();
    } catch (Exception e) {
      rows = null;
    }
    return rows;
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
