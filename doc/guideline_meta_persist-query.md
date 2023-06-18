

### 单表查询

单表查询语句的通用结构如下：
```SQL
SELECT (DISTINCT) ATTR_NAME1, ATTR_NAME2, ..., ATTR_NAMEN 
FROM TABLE_NAME 
WHERE ATTR_NAME_K COMPARATOR ATTR_VALUE_K;
```

具体地：
- 各个ATTR_NAME为要查询的属性名称，也可用SELECT * 查询表中符合条件的记录的所有属性。
- 本数据库支持DISTINCT关键字，可在查询时对返回的属性集进行去重。
- TABLE_NAME为要查询的单表名称
- WHERE语句中的"ATTR_NAME_K COMPARATOR ATTR_VALUE_K"为查询的限定条件，其中COMPARATOR可取EQ、NE、GT、LT、GE、LE六个取值。

>示例：`SELECT * FROM STUDENT WHERE AGE>18;`

>示例：`SELECT DISTINCT NAME FROM STUDENT WHERE AGE>18;`

---

### 多表连接查询

多表连接查询的通用结构如下：
```SQL
SELECT (DISTINCT) ATTR_NAME1, ATTR_NAME2, ..., ATTR_NAMEN
FROM TABLE_NAME_1 JOIN TABLE_NAME_2 
ON ATTR_NAME_I = ATTR_NAME_J
WHERE ATTR_NAME_K COMPARATOR ATTR_VALUE_K;
```

>示例：`SELECT * FROM STUDENT JOIN GRADE ON STUDENT.ID=GRADE.ID where GRADE.GPA>3.5`

---