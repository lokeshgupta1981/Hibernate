# Bootstrapping EntityManager in Hibernate

Source code for the article [Bootstrapping EntityManager in Hibernate](https://howtodoinjava.com/hibernate/bootstrap-hibernate-entitymanager/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # creates each factory and prints the SQL for each step
mvn test                   # asserts every result shown in the article
```

## Files

| File | What it shows |
|---|---|
| Drink.java | The entity: name, size and price of a drink on a coffee shop menu |
| META-INF/persistence.xml | The `coffee-shop` persistence unit read by `Persistence.createEntityManagerFactory()` |
| EntityManagerFactories.java | Five ways to create an `EntityManagerFactory`: persistence.xml, persistence.xml with runtime overrides, `PersistenceConfiguration`, `HibernatePersistenceConfiguration`, `PersistenceUnitInfo` with `createContainerEntityManagerFactory()` |
| CoffeeShopUnitInfo.java | A `PersistenceUnitInfo` implementation, the object a framework passes to the provider |
| CoffeeShopDatabase.java | One shared `EntityManagerFactory` per application, closed by a shutdown hook |
| EntityManagerDemo.java | Runs each step: `runInTransaction()`, `callInTransaction()`, manual transactions, rollback, closing |
| EntityManagerBootstrapTest.java | JUnit tests for every behavior described in the article |
