package cn.edu.thssdb.schema;

import cn.edu.thssdb.index.BPlusTree;
import cn.edu.thssdb.utils.Pair;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Table implements Iterable<Row>, Serializable {
  ReentrantReadWriteLock lock;
  private String databaseName;
  public String tableName;
  public ArrayList<Column> columns;
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
      if (this.columns.get(i).get_Primary() == 1) {
        primaryIndex = i;
      }
    }
    recover();
  }

  private void recover() {
    // TODO
  }

  public void insert(Row row) {
    // TODO
    try {
      lock.writeLock().lock();
      Entry key = row.getEntries().get(primaryIndex);
      index.put(key, row);
      //      serialize();
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void delete(Entry key) {
    // TODO
    try {
      lock.writeLock().lock();
      index.remove(key);
      //      serialize();
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void update(Entry key, Row newRow) {
    // TODO
    try {
      lock.writeLock().lock();
      index.remove(key);
      index.put(newRow.getEntries().get(primaryIndex), newRow);
      //      serialize();
    } finally {
      lock.writeLock().unlock();
    }
  }

  private void serialize(ArrayList<Row> rows, String filename) throws IOException {
    // TODO
    ObjectOutputStream o_stream = new ObjectOutputStream(new FileOutputStream(filename));
    o_stream.writeObject(rows);
    o_stream.close();
  }

  private ArrayList<Row> deserialize() {
    // TODO
    return null;
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
