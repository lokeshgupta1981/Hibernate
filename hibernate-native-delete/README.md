# JPA Native DELETE Query: @NamedNativeQuery Example

Source code for the article [JPA Native DELETE Query: @NamedNativeQuery Example](https://howtodoinjava.com/jpa/jpa-native-delete-sql-query-example/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # prints the SQL for each case
mvn test                   # asserts every result shown in the article
```

## Files

| File | What it shows |
|---|---|
| Coupon.java | Parent entity with two `@NamedNativeQuery` DELETE statements, a `@OneToMany` to redemptions and a `@PreRemove` callback |
| Redemption.java | Child entity that owns the `coupon_id` foreign key (`fk_redemption_coupon`) |
| Campaign.java | `@SoftDelete` entity: `remove()` and JPQL mark rows deleted, a native DELETE removes them |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration`, seeds the coupons and recreates the foreign key with `ON DELETE CASCADE` |
| NativeDeleteDemo.java | Runs each case and prints the SQL |
| NativeDeleteTest.java | 15 JUnit tests for every behavior described in the article |
