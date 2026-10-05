# Guide to Pagination with Hibernate

Source code for the article [Guide to Pagination with Hibernate](https://howtodoinjava.com/hibernate/hibernate-pagination/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn compile exec:java   # prints the SQL and the result of each step (without -q, to see the HHH90003004 warning)
mvn test                # asserts every result shown in the article
```

## Files

| File | What it shows |
|---|---|
| JobPosting.java | Entity that we page: title, city, postedOn, salary and a `@ManyToOne` company |
| Company.java | Entity with a `@OneToMany` postings collection, used for the join fetch cases |
| JobBoard.java | Every paging technique: `setFirstResult()`/`setMaxResults()`, count queries, `getResultCount()`, `Page`, manual keyset, `KeyedPage`/`KeyedResultList`, Criteria API, join fetch and the two-query fix, `ScrollableResults`, `getResultStream()`, HQL `limit`/`offset`, native query |
| PageResult.java | Record with the page content and the totals for a pager |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` and saves 3 companies with 12 job postings |
| NoOffsetInSubqueryH2Dialect.java | H2 dialect without OFFSET in subqueries, to reproduce the in-memory paging of Hibernate 7.3 and older |
| SqlCapture.java | `StatementInspector` that records the SQL so the tests can check it |
| PaginationDemo.java | Runs each step and prints the SQL |
| PaginationTest.java | 19 JUnit tests for every result in the article |
