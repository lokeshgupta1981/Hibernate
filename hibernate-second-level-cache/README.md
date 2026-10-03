# Guide to Hibernate Second Level Cache

Source code for the article [Guide to Hibernate Second Level Cache](https://howtodoinjava.com/hibernate/how-hibernate-second-level-cache-works/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2) with hibernate-jcache 7.4.11.Final
- Ehcache 3.12.0 (jakarta classifier) as the JCache provider
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # prints the SQL and the cache statistics for each step
mvn test                   # asserts every result shown in the article
```

## Files

| File | What it shows |
|---|---|
| Country.java | Entity with `@Cacheable` and `@Cache(usage = READ_WRITE)`, plus a cached `@ManyToMany` currencies collection |
| Currency.java | `@Immutable` entity cached with `READ_ONLY` |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` and turns on the JCache region factory, the query cache and statistics |
| ehcache.xml | Ehcache regions for the entities, the collection and the query cache |
| SecondLevelCacheDemo.java | Runs each step and prints the SQL, the cache statistics and the raw cache entries |
| SecondLevelCacheTest.java | JUnit tests for every behavior described in the article |
| Language.java, Continent.java, Ocean.java, Timezone.java, Landmark.java (test) | Test entities for `@Cacheable` alone, `@Cache` alone, no caching, `READ_ONLY` without `@Immutable` and `NONSTRICT_READ_WRITE` |
