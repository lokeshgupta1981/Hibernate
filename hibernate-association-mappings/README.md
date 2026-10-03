# Hibernate Association Mappings and JPA Entity Relationships

Source code for the article [Hibernate Association Mappings and JPA Entity Relationships](https://howtodoinjava.com/hibernate/how-to-define-association-mappings-between-hibernate-entities/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # prints the DDL and the SQL for each step
mvn test                   # asserts every result shown in the article
```

## Files

| File | What it shows |
|---|---|
| Member.java | Inverse side of `@OneToOne` card and `@OneToMany` workouts (`mappedBy`), owning side of `@ManyToMany` classes (`@JoinTable member_class`) |
| MembershipCard.java | Owning side of the one-to-one: holds the `member_id` foreign key |
| Workout.java | Owning side of the one-to-many: `@ManyToOne(fetch = LAZY)` with the `member_id` foreign key |
| FitnessClass.java | Inverse side of the many-to-many (`mappedBy = "classes"`) |
| Trainer.java | Unidirectional `@OneToMany` without `mappedBy`: Hibernate creates the `Trainer_FitnessClass` join table |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` (no persistence.xml) |
| AssociationMappingsDemo.java | Runs each step and prints the SQL |
| AssociationMappingsTest.java | JUnit tests for every behavior described in the article |
