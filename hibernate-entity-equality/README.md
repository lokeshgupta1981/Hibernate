# Hibernate Entity Equality

Source code for the article [Checking Hibernate Entity Equality between Sessions](https://howtodoinjava.com/hibernate/hibernate-entities-equality-and-identity/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- Lombok 1.18.48 (only for the Lombok examples)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # prints each step with the SQL
mvn test                   # asserts every result shown in the article
```

## Files

Every variant maps its own `Member` class (id, email, name) to the `Member` table, each in its own in-memory database.

| File | What it shows |
|---|---|
| plain/Member.java | No `equals()`/`hashCode()`: the `Object` defaults |
| naive/Member.java | `Objects.equals(id)` and `Objects.hash(id)`: two new members are equal, and the hash code changes after persist |
| byid/Member.java | `equals()` on a non-null id with a constant `hashCode()` |
| email/Member.java | Business key: `equals()` and `hashCode()` on the `@NaturalId` email |
| uuid/Member.java | `UUID` id assigned when the object is created |
| proxy/Member.java | IDE-generated `equals()` with `getClass()` and field access, which fails for proxies |
| lombok/Member.java, Visit.java | Lombok `@Data` on entities and what breaks |
| lombokfixed/Member.java, Visit.java | Lombok with `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` on the email |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` (no persistence.xml) |
| EntityEqualityDemo.java | Runs each case and prints the SQL |
| EntityEqualityTest.java | 27 JUnit tests for every behavior described in the article |
