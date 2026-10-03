# Hibernate Soft Delete: @SoftDelete Example

Source code for the article [Hibernate Soft Delete: @SoftDelete Example](https://howtodoinjava.com/hibernate/soft-delete-annotation-example/).

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
| Bookmark.java | `@SoftDelete` with the default `deleted` boolean column, and a soft-deleted `@ElementCollection` of tags |
| BookmarkFolder.java | `@SoftDelete(strategy = TIMESTAMP)` and a cascading `@OneToMany` to bookmarks |
| ActiveBookmark.java | `strategy = ACTIVE` (column `active`) |
| YesNoBookmark.java | `columnName = "removed"` with `YesNoConverter` |
| LegacyBookmark.java | The older approach: `@SQLDelete` + `@SQLRestriction` with a mapped `deleted` field |
| UniqueNameFolder.java | `@Column(unique = true)` that blocks reusing a deleted name |
| OneToManySoftDeleteFolder.java | Wrong mapping: `@SoftDelete` on `@OneToMany` (fails at startup) |
| TimestampConverterBookmark.java | Wrong mapping: a converter with `TIMESTAMP` (fails at startup) |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` (no persistence.xml), one H2 database per mapping |
| SoftDeleteDemo.java | Runs each step and prints the SQL |
| SoftDeleteTest.java | JUnit tests for every behavior described in the article |
| src/test/.../Profile.java and others | Test-only mappings: `@ManyToMany` join table, `List` element collection, plain (non-soft-deleted) folder, read-only flag field, hard delete |
