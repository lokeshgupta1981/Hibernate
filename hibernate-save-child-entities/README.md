# How to Save Child Entities using Hibernate?

Source code for the article [How to Save Child Entities using Hibernate?](https://howtodoinjava.com/hibernate/automatically-save-child-entities/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # prints the SQL for each step
mvn test                   # asserts every result shown in the article
```

## Files

| File | What it shows |
|---|---|
| Survey.java | Parent entity: `@OneToMany(mappedBy = "survey", cascade = {PERSIST, MERGE})` and the `addQuestion()` helper |
| Question.java | Child entity that owns the `survey_id` foreign key (`@ManyToOne`) |
| nocascade/Survey.java, nocascade/Question.java | The same model without cascade |
| unidirectional/Survey.java, unidirectional/Question.java | Unidirectional `@OneToMany` with `@JoinColumn` (extra `UPDATE` per child) |
| identity/Survey.java, identity/Question.java | `GenerationType.IDENTITY` with `CascadeType.ALL` |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` (no persistence.xml) |
| SqlLog.java | `StatementInspector` that records the SQL, used by the tests |
| SaveChildDemo.java, NoCascadeDemo.java | Run each case and print the SQL |
| SaveChildEntitiesTest.java | JUnit tests for every behavior described in the article |
