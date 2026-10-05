# Hibernate One-to-Many Mapping

Source code for the article [Hibernate One-to-Many Mapping: JPA @OneToMany Explained](https://howtodoinjava.com/hibernate/hibernate-one-to-many-mapping/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # prints the SQL for each mapping and step
mvn test                   # asserts every result shown in the article
```

## Files

| File | What it shows |
|---|---|
| bidirectional/Account.java | Parent with `@OneToMany(mappedBy = "account")`, `@OrderBy` and the `addPayment()` / `removePayment()` helpers |
| bidirectional/Payment.java | Child with the owning `@ManyToOne(fetch = LAZY)` and `@JoinColumn(name = "account_id")` |
| joincolumn/Account.java, Payment.java | Unidirectional `@OneToMany` with `@JoinColumn` (extra UPDATE statements) |
| jointable/Account.java, Payment.java | Unidirectional `@OneToMany` without `@JoinColumn` (Account_Payment join table) |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` (no persistence.xml) |
| SqlLog.java | `StatementInspector` that records the SQL so the tests can count statements |
| OneToManyDemo.java | Runs each step and prints the SQL, including N+1, join fetch and batch fetching |
| OneToManyTest.java | JUnit tests for every statement count and behavior described in the article |
