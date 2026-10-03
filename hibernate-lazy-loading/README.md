# Guide to Lazy Loading in Hibernate

Source code for the article [Guide to Lazy Loading in Hibernate](https://howtodoinjava.com/hibernate/lazy-loading-in-hibernate/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q process-classes exec:java   # prints the SQL and the statement count of each step
mvn test                           # asserts every result shown in the article
```

`process-classes` (or any later phase) is needed because `hibernate-maven-plugin` enhances `MenuCard` after compilation.

## Files

| File | What it shows |
|---|---|
| Restaurant.java | `@OneToMany` menu, LAZY by default (a `PersistentBag`), and the `Restaurant.menu` named entity graph |
| MenuItem.java | `@ManyToOne(fetch = LAZY)` restaurant (a proxy), and `@Basic(fetch = LAZY)` that is ignored without enhancement |
| DailySpecial.java | `@ManyToOne` with the default EAGER fetch type |
| MenuCard.java | Bytecode-enhanced entity whose `@Lob @Basic(fetch = LAZY)` text loads on first access |
| MenuLine.java | Record used as a DTO projection |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration`, statistics on, and seeds three restaurants |
| LazyLoadingDemo.java | Runs each step: lazy collection, proxy, LazyInitializationException, join fetch, entity graph, Hibernate.initialize(), DTO, N+1, batch fetching, enable_lazy_load_no_trans |
| LazyLoadingTest.java | 27 JUnit tests for every behavior described in the article |
