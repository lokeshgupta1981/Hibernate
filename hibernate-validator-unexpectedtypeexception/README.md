# No Validator Could Be Found for Constraint: Fix HV000030

Source code for the article [No Validator Could Be Found for Constraint: Fix HV000030](https://howtodoinjava.com/hibernate/unexpectedtypeexception-error/).

## Versions

- Java 25
- Hibernate Validator 9.1.4.Final (Jakarta Validation 3.1)
- Eclipse Expressly 6.0.0 (Jakarta Expression Language implementation)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # triggers each cause and prints the exception or the violations
mvn test                   # asserts every message and result shown in the article
```

## Files

| File | What it shows |
|---|---|
| GiftCardOrder.java | The corrected class: every constraint matches the type of its field |
| Recipient.java | Our own type, with constraints on its String components |
| WrongOrders.java | One small class per mistake, the cases that look wrong but work, and the fixes |
| ValidRecipient.java | Custom constraint annotation for the `Recipient` type |
| RecipientValidator.java | `ConstraintValidator<ValidRecipient, Recipient>` that checks a recipient |
| Validators.java | Builds the `ValidatorFactory` and formats violations |
| UnexpectedTypeDemo.java | Runs each case and prints the result |
| UnexpectedTypeTest.java | Reproduces each `UnexpectedTypeException` and proves each fix, plus the compile-time check with the annotation processor |
| TypeGrid.java, TypeGridTest.java | Builds the type to constraint compatibility grid by validating every combination |
| LookupTableTest.java | Checks the other cells of the lookup table (Long, @Digits, @Pattern, Set, Map, LocalDateTime, Instant) |
