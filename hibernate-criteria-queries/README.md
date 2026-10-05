# Guide to Hibernate Criteria Queries

Source code for the article [Guide to Hibernate Criteria Queries](https://howtodoinjava.com/hibernate/hibernate-criteria-queries-tutorial/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- hibernate-processor 7.4.11.Final (generates the static metamodel `Listing_`, `Neighborhood_`, `Amenity_`)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # runs every Criteria query and prints the SQL
mvn test                   # asserts every result and SQL statement shown in the article
```

## Files

| File | What it shows |
|---|---|
| Listing.java | Entity with title, price, bedrooms, listedOn, status, `@ManyToOne` neighborhood and `@ManyToMany` amenities |
| Neighborhood.java | Entity with name and city |
| Amenity.java | Entity shared by many listings through the `listing_amenity` join table |
| ListingStatus.java | Enum ACTIVE, PENDING, SOLD |
| ListingQueries.java | Every Criteria query of the article: where, and/or, order by, projections, joins, fetch joins, group by, subqueries, pagination, dynamic filters, `CriteriaUpdate`, `CriteriaDelete`, metamodel, `HibernateCriteriaBuilder` |
| ListingFilter.java | Optional search inputs for the dynamic filter |
| ListingSummary.java, NeighborhoodStats.java | Records filled with `cb.construct()` |
| PageResult.java | One page of results plus the total count |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` and saves the sample listings |
| SqlCapture.java | `StatementInspector` that records the SQL so the tests can assert it |
| CriteriaDemo.java | Runs each query and prints the SQL |
| CriteriaQueriesTest.java | JUnit tests for every result, SQL statement and error in the article |
