# Hibernate EhCache Configuration Tutorial

Source code for the article [Hibernate EhCache Configuration Tutorial](https://howtodoinjava.com/hibernate/hibernate-ehcache-configuration-tutorial/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2) with hibernate-jcache 7.4.11.Final
- Ehcache 3.12.0 (jakarta classifier) as the JCache provider
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn compile exec:java   # prints the SQL, the cache settings and the statistics of each step (without -q, so the Hibernate warnings show)
mvn test                # asserts every result shown in the article (the expiry tests wait about 6 seconds)
```

## Files

| File | What it shows |
|---|---|
| Language.java | Entity cached with `READ_WRITE` in the default region (its class name), plus a cached `@OneToMany` translations collection |
| Translation.java | Entity cached in the custom region `translations` with `@Cache(region = "translations")` |
| ReadOnlyLabel.java, NonstrictLabel.java, ReadWriteLabel.java, TransactionalLabel.java, Label.java | One small entity per `CacheConcurrencyStrategy` |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` and the JCache region factory; reads cache names, sizes and expiry back from Ehcache |
| ehcache.xml | Main configuration: a template, one cache per region, ttl, tti and none expiry |
| ehcache-expiry.xml | 1 second ttl and 2 second tti to watch expiry happen |
| ehcache-small-heap.xml | Heap of 2 entries, with and without an offheap tier |
| ehcache-size.xml | Heap sized in kB instead of entries |
| ehcache-missing.xml | Regions left out, for `hibernate.javax.cache.missing_cache_strategy` |
| ehcache-default-template.xml | `jsr107:defaults default-template` for regions without a cache element |
| ehcache-typed.xml | `key-type` and `value-type` that do not match Hibernate's cache keys (fails at commit) |
| ehcache-labels.xml | Caches for the four concurrency strategy entities |
| EhcacheConfigDemo.java | Runs each step and prints the SQL, the cache settings and the statistics |
| EhcacheConfigTest.java | JUnit tests for every behavior described in the article |
