# Cascade Types in JPA and Hibernate

Source code for the article [Cascade Types in JPA and Hibernate](https://howtodoinjava.com/hibernate/hibernate-jpa-cascade-types/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # prints the SQL for every cascade case
mvn test                   # asserts every result shown in the article
```

## Files

Each package holds the same `Album` (one) and `Photo` (many) entities with a different `cascade` setting, so every case runs against its own mapping.

| Package / file | What it shows |
|---|---|
| `persist` | `cascade = CascadeType.PERSIST` |
| `merge` | `cascade = CascadeType.MERGE` |
| `remove` | `cascade = CascadeType.REMOVE` |
| `refresh` | `cascade = CascadeType.REFRESH` |
| `detach` | `cascade = CascadeType.DETACH` |
| `all` | `cascade = CascadeType.ALL` |
| `persistmerge` | `cascade = {CascadeType.PERSIST, CascadeType.MERGE}` |
| `none` | No cascade, for comparison |
| `lock` | Hibernate `@Cascade(org.hibernate.annotations.CascadeType.LOCK)` |
| `ondelete` | Hibernate `@OnDelete(action = OnDeleteAction.CASCADE)` (database-level delete) |
| `wrong` | `CascadeType.REMOVE` on `@ManyToOne` and `@ManyToMany` (what not to do) |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` (no persistence.xml) |
| SqlLog.java | `StatementInspector` that records the SQL so the tests can check it |
| CascadeDemo.java | Runs each case and prints the SQL |
| CascadeTypesTest.java | JUnit tests for every behavior described in the article |
