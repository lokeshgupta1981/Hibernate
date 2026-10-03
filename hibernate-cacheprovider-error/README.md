# [SOLVED] java.lang.NoClassDefFoundError: Lorg/hibernate/cache/CacheProvider

Source code for the article [[SOLVED] java.lang.NoClassDefFoundError: Lorg/hibernate/cache/CacheProvider](https://howtodoinjava.com/hibernate/solved-java-lang-noclassdeffounderror-lorghibernatecachecacheprovider/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2) with hibernate-jcache 7.4.11.Final
- Ehcache 3.12.0 (classifier `jakarta`)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer
- Test scope only, to reproduce the error: hibernate-ehcache 3.6.10.Final and Spring 3.2.18 (spring-orm, spring-beans, spring-tx, spring-core) without their transitive dependencies

## Run

```bash
mvn -q test-compile exec:java   # prints the errors and the cache statistics of the Hibernate 7 setup
mvn test                        # asserts every error and result shown in the article
```

## Files

| File | What it shows |
|---|---|
| PostalCode.java | Entity cached with `@Cacheable` and `@Cache(usage = READ_WRITE)` |
| Database.java | Bootstraps Hibernate 7 with `HibernatePersistenceConfiguration`, JCache and Ehcache 3 |
| ehcache.xml | One Ehcache cache per Hibernate region |
| CacheProviderErrorDemo.java | Shows the error from Spring's `orm.hibernate3.LocalSessionFactoryBean`, the startup errors of old region factory names, and the cache hits of the working setup |
| CacheProviderErrorTest.java | JUnit tests: `NoClassDefFoundError` from old jars, old settings on Hibernate 7, and the Hibernate 7 cache proven with `Statistics` |
