# [Solved] org.hibernate.exception.GenericJDBCException: could not prepare statement

Source code for the article [[Solved] org.hibernate.exception.GenericJDBCException: could not prepare statement](https://howtodoinjava.com/hibernate/solved-org-hibernate-exception-genericjdbcexception-could-not-prepare-statement/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2) and hibernate-community-dialects 7.4.11.Final (SQLite dialect)
- H2 2.5.252, SQLite JDBC 3.53.4.0, HSQLDB 2.7.4 (all in-memory)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # runs each failure and fix, prints the SQL and the root SQLException
mvn test                   # asserts every exception and message shown in the article

# reproduce the old HSQLDB "This function is not supported" error with the 1.8 driver
mvn test -Dhsqldb.groupId=hsqldb -Dhsqldb.version=1.8.0.10
```

## Files

| File | What it shows |
|---|---|
| CakeOrder.java | Correct mapping: `@Table(name = "cake_order")` and `@Column(name = "order_value")` |
| Order.java | Broken mapping: table `Order` and column `value` are SQL keywords |
| IdentityOrder.java | `IDENTITY` id, used for the old HSQLDB driver case |
| Database.java | Bootstraps H2, SQLite and HSQLDB with `HibernatePersistenceConfiguration` |
| RootCause.java | Finds the `JDBCException` and prints SQL, SQLState and error code of the `SQLException` |
| GenericJdbcDemo.java | Runs every failure and fix and prints the SQL |
| GenericJdbcExceptionTest.java | 19 JUnit tests for every exception, message and fix in the article |
