### 查询模块

1. **表查找逻辑——`QueryTable`类的实现**

本项目首先自顶向下地实现了`QueryTable`类，该类的本质是构建一个在查询期间使用的临时表，用于区分及处理单表和多表连接的查询场景。

基于单表查询和多表查询两个场景，本项目将`QueryTable`类重定义为了一个抽象类，并为其实现了`SingleQueryTable`和`MultipleQueryTable`两个子类，分别处理单表查询和多表连接查询。

`QueryTable`抽象类的几个核心函数如下：

```java
  public abstract void findAndAddNext(); // 找到下一个符合查询条件的行，并将其添加至当前的临时查询表

  public abstract ArrayList<MetaInfo> GenerateMetaInfo();  // 生成当前查询表的表名和列信息两类元数据

  @Override
  public boolean hasNext() {  // 判断当前查询表是否已读完
    if (!row_queue.isEmpty() || first_flag) return true;
    return false;
  }

  // return next row
  @Override
  public QueryRow next() {  // 返回当前查询表的下一行
    if (row_queue.isEmpty()) {
      findAndAddNext();
      System.out.println("QueryTable next(): row_queue.isEmpty() -> findAndAddNext done"); // debug
      if (first_flag) first_flag = false;
    }

    QueryRow res_row = null;
    if (!row_queue.isEmpty()) res_row = row_queue.poll();
    else return null;
    if (row_queue.isEmpty()) findAndAddNext();

    System.out.println("QueryTable next(): !row_queue.isEmpty() -> findAndAddNext done"); // debug

    return res_row;
  }
```

基于`QueryTable`类的方法定义，`SingleQueryTable`和`MultipleQueryTable`两个子类分别对`QueryTable`的两个抽象方法进行了具体的实现。总体而言，`SingleQueryTable`是直接对要查询的单表展开查询搜索，并将符合的行加入到当前的临时查询表当中；而`MultipleQueryTable`则是先基于限定条件将要查询的多个表连接成一个单个查询表，然后再返回查询答案。为了实现`MultipleQueryTable`类，我们还实现了一个`QueryRow`类，用于将原本位于多个表中的行数据连接成一个能加入临时查询表的单行。

2. **WHERE语句的执行——`MultipleCondition`、`Condition`、`Expression`类的实现**

官方文件SQL.g4的SELECT语句结构如下：

```SQL
selectStmt :
    K_SELECT ( K_DISTINCT | K_ALL )? resultColumn ( ',' resultColumn )*
        K_FROM tableQuery ( ',' tableQuery )* ( K_WHERE multipleCondition )? ;
```

更进一步地，其将multipleCondition递归定义如下：

```SQL
multipleCondition :
    condition
    | multipleCondition AND multipleCondition
    | multipleCondition OR multipleCondition ;
```

```SQL
condition :
    expression comparator expression;
```

```SQL
expression :
    comparer
    | expression ( MUL | DIV ) expression
    | expression ( ADD | SUB ) expression
    | '(' expression ')';
```

```SQL
comparer :
    columnFullName
    | literalValue ;
```

基于SQL.g4的对WHERE模块的递归定义，我们实现了对应的三个嵌套类`multipleCondition`、`Condition`和`Expression`。

完成WHERE查询时，我们递归调用三个类各自的`executeQuery`方法，通过该方法的层层执行与传递获取整个查询语句WHERE模块的查询结果。

（本项目的Expression类并未实现SQL.g4中带中间运算符的完整逻辑，而只是完成了基础功能的要求，即expression的实现等同于comparer的实现）

