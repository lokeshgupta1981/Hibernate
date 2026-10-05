# Java Bean Validation using Hibernate Validator

Source code for the article [Java Bean Validation using Hibernate Validator](https://howtodoinjava.com/hibernate/hibernate-validator-java-bean-validation/).

## Versions

- Java 25
- Hibernate Validator 9.1.4.Final (Jakarta Validation 3.1)
- Expressly 6.0.0 (Jakarta Expression Language 6.0 implementation)
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2), only for the persist example
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # validates each example and prints the violations
mvn test                   # asserts every result shown in the article
```

## Files

| File | What it shows |
|---|---|
| Attendee.java | Entity with field constraints, `@Valid` address, `List<@Email String> guestEmails`, a `Checkout` group rule and the class-level `@StudentTicket` |
| Address.java | Embeddable validated through `@Valid` |
| TicketType.java | Ticket types: STANDARD, STUDENT, VIP |
| Checkout.java | Validation group for rules checked at payment |
| RegistrationChecks.java | `@GroupSequence` that checks Default first, then Checkout |
| StudentTicket.java, StudentTicketValidator.java | Custom class-level constraint and its `ConstraintValidator` |
| Talk.java | Java record with constraints on its components |
| BadgeText.java | Compares `@NotNull`, `@NotEmpty` and `@NotBlank` |
| RegistrationService.java | Method parameter and return value constraints |
| Validators.java | Builds the default and the fail-fast `Validator` |
| Database.java | Bootstraps Hibernate ORM with `HibernatePersistenceConfiguration` (no persistence.xml) |
| ValidationMessages.properties, ValidationMessages_de.properties | Custom message templates in English and German |
| BeanValidationDemo.java | Runs each example and prints the violations and SQL |
| BeanValidationTest.java | JUnit tests for every behavior described in the article |
| Sponsor.java (test) | Entity with an IDENTITY id, to show that the check then runs inside persist() |
