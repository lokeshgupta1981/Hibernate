# save(), update() and saveOrUpdate() on Hibernate 6.6

Source code for the article [Hibernate save(), update() and saveOrUpdate()](https://howtodoinjava.com/hibernate/hibernate-save-and-saveorupdate/).

This project uses Hibernate ORM 6.6.58.Final, because Hibernate 7 removed these methods. It exists only to show how the old methods behaved; new code uses `persist()` and `merge()` (see the parent folder).

## Run

```bash
mvn -q compile exec:java   # prints the SQL of each old call, at the call and at commit
mvn test                   # 15 JUnit tests
```
