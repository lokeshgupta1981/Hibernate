# Hibernate Annotations vs XML Mappings

Source code for the article [Hibernate Annotations vs XML Mappings](https://howtodoinjava.com/hibernate/pros-and-cons-of-hibernate-annotations-vs-mappings/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # maps the same Bicycle five ways and prints the SQL for each
mvn test                   # asserts every table and column shown in the article
```

## Files

| File | What it shows |
|---|---|
| Bicycle.java | Entity mapped with Jakarta Persistence annotations |
| plain/Bicycle.java | The same class without annotations, mapped only in XML |
| META-INF/bicycle-orm.xml | Full `orm.xml` mapping of the plain class |
| META-INF/bicycle-override.xml | `orm.xml` that overrides the table and one column of the annotated class (`metadata-complete="false"`) |
| META-INF/bicycle-complete.xml | `orm.xml` with `metadata-complete="true"`: all annotations are ignored |
| Bicycle.hbm.xml | Legacy Hibernate `hbm.xml` mapping (deprecated, still runs on 7.4 with warning HHH90000028) |
| Database.java | Builds one `EntityManagerFactory` per mapping style with `HibernatePersistenceConfiguration` (and `Configuration` for hbm.xml) |
| MappingDemo.java | Runs each style and prints the DDL, the insert and the resulting columns |
| test: META-INF/bicycle-typo.xml | `orm.xml` that names a field that does not exist, for the startup error test |
| MappingStylesTest.java | JUnit tests for every result described in the article |
