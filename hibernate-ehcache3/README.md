# Configuring Ehcache 3 with Hibernate 6 and 7

Source code for the article [Configuring Ehcache 3 with Hibernate 6](https://howtodoinjava.com/hibernate/configuring-ehcache-3-with-hibernate-6/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final with hibernate-jcache (Jakarta Persistence 3.2)
- Ehcache 3.12.0 (classifier `jakarta`)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # prints the SQL, the cache statistics and the startup errors
mvn test                   # asserts every result shown in the article
```

## Files

| File | What it shows |
|---|---|
| pom.xml | `hibernate-jcache` and `ehcache` with the `jakarta` classifier and the JAXB 2 exclusion |
| Airport.java | Cached entity with `@Cacheable` and `@Cache(usage = READ_WRITE)` |
| Airline.java | Cached entity with a cached `@ManyToMany` collection of hub airports |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` and the second level cache properties |
| ehcache.xml | One cache per Hibernate region, with a size limit and a time to live |
| ehcache-no-hubs.xml | The same file without the collection region, for the `missing_cache_strategy` example |
| EhcacheDemo.java | Two sessions per lookup with the SQL and the statistics, a cached query, and the startup errors |
| EhcacheSetupTest.java | JUnit tests for the cache hits, the query cache and every configuration error |
| ClasspathErrorsTest.java, StartCheck.java | Start Hibernate in a new JVM without `hibernate-jcache`, or with the ehcache jar without the `jakarta` classifier |
