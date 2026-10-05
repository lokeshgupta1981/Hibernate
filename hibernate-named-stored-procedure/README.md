# Hibernate @NamedStoredProcedureQuery

Source code for the article [Hibernate @NamedStoredProcedureQuery](https://howtodoinjava.com/hibernate/jpa-21-namedstoredprocedurequery-annotation-example/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- MySQL 9 (started in Docker by Testcontainers 2.0.5), MySQL Connector/J 26.7.0
- Spring Data JPA 4.1.1 (only for the `@Procedure(name = ...)` repository example)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

Docker must be running, because Testcontainers starts a MySQL container.

```bash
mvn -q compile exec:java   # prints each call, its SQL and result, and the error messages
mvn test                   # asserts every result shown in the article
```

## Files

| File | What it shows |
|---|---|
| procedures.sql | The MySQL procedures: `count_units`, `find_low_stock`, `stock_summary`, `restock`, `stock_audit` |
| StockItem.java | Entity with all `@NamedStoredProcedureQuery` declarations (OUT, positional, `resultClasses`, `resultSetMappings`, INOUT, timeout hint) and the ones used for the error examples |
| WarehouseSummary.java | Record filled through `@SqlResultSetMapping` and `@ConstructorResult` |
| StockProcedures.java | Calls each declaration by name, plus the JDBC timeout workaround |
| StockItemRepository.java | Spring Data JPA repository method with `@Procedure(name = "StockItem.countUnits")` |
| Database.java | Starts MySQL with Testcontainers, bootstraps Hibernate with `HibernatePersistenceConfiguration` and creates the procedures |
| NamedProcedureDemo.java | Runs each call and prints the SQL, the results and the errors |
| NamedProcedureTest.java | JUnit tests for every result and error message shown in the article |
