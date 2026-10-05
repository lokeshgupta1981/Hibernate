# Bootstrapping ValidatorFactory with Hibernate Validator CDI

Source code for the article [Bootstrapping ValidationFactory with Hibernate Validator CDI](https://howtodoinjava.com/hibernate/hibernate-validator-cdi/).

## Versions

- Java 25
- Hibernate Validator 9.1.4.Final with hibernate-validator-cdi (Jakarta Validation 3.1)
- Weld SE 6.0.4.Final (Jakarta CDI 4.1)
- Expressly 6.0.0 (Jakarta Expression Language, for message interpolation)
- Hibernate ORM 7.4.11.Final and H2 2.5.252 (only to show entity validation with the CDI factory)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # prints each step: plain factory, injection, method validation, Hibernate ORM
mvn test                   # asserts every result shown in the article
```

## Files

| File | What it shows |
|---|---|
| TaxFiling.java | Entity with field constraints and the custom `@RegisteredTaxpayer` constraint |
| RegisteredTaxpayer.java | Custom constraint annotation |
| RegisteredTaxpayerValidator.java | `ConstraintValidator` with an `@Inject` field (`TaxpayerRegistry`) |
| TaxpayerRegistry.java | CDI bean used by the validator |
| FilingInspector.java | Injects `Validator` and `ValidatorFactory` with `@Default` and `@HibernateValidator` |
| TaxFilingService.java | CDI bean with parameter and return value constraints (method validation) |
| FixedClockProvider.java | `ClockProvider` registered in `validation.xml` |
| Database.java | Bootstraps Hibernate ORM with `HibernatePersistenceConfiguration` and passes the CDI `ValidatorFactory` |
| META-INF/beans.xml | Marks the jar as a CDI bean archive for Weld SE |
| META-INF/validation.xml | Default provider and clock provider |
| ValidatorCdiDemo.java | Runs each step and prints the output |
| ValidatorCdiTest.java | JUnit tests with Weld SE for every behavior described in the article |
