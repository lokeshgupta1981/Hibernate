# NotYetImplementedException on Hibernate 4.1

Reproduces `org.hibernate.cfg.NotYetImplementedException: Pure native scalar queries are not yet supported`
for the article [Pure Native Scalar Queries Are Not Yet Supported: Fix](https://howtodoinjava.com/hibernate/workaround-jpa-hibernate-notyetimplementedexception-pure-native-scalar-queries-are-not-yet-supported/).

- hibernate-entitymanager 4.1.12.Final (javax.persistence, JPA 2.0), H2 1.4.200, JUnit 6.1.3
- Runs on Java 25, but compiles to Java 8 bytecode, because the javassist 3.15 class scanner of Hibernate 4.1
  cannot read newer class files (`invalid constant type: 18`).
- `.mvn/jvm.config` and the surefire `argLine` open `java.lang` to javassist, which Hibernate 4.1 uses for proxies.

```bash
mvn -q compile exec:java
mvn test
mvn test -Dhibernate.version=3.6.10.Final   # also throws, wrapped in "Unable to configure EntityManagerFactory"
mvn test -Dhibernate.version=4.2.0.Final    # no longer throws (HHH-4412)
```
