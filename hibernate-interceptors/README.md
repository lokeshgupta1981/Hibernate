# Guide to Hibernate Interceptors

Source code for the article [Guide to Hibernate Interceptors](https://howtodoinjava.com/hibernate/hibernate-interceptors/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # prints the SQL and the interceptor calls for each step
mvn test                   # asserts every result shown in the article
```

## Files

| File | What it shows |
|---|---|
| Appointment.java | Entity: patient name, doctor, slot and status of a clinic appointment |
| AuditLog.java | Audit trail row: action, property, old value and new value |
| AuditInterceptor.java | Session-scoped interceptor: `onPersist` (fills a default status), `onFlushDirty` (old and new values), `onRemove`, and `postFlush` writing the audit rows on the same connection |
| LoggingInterceptor.java | Stateless interceptor shared by every session (`hibernate.session_factory.interceptor`) |
| TracingInterceptor.java | Prints the order in which Hibernate calls the interceptor |
| SqlTagInspector.java | `StatementInspector` that adds a comment to every SQL statement |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration`, with each way to register an interceptor or inspector |
| InterceptorDemo.java | Runs each step and prints the SQL |
| InterceptorTest.java | JUnit tests for every behavior described in the article |
