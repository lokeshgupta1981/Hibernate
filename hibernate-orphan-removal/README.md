# Hibernate Orphan Removal

Source code for the article [Hibernate Orphan Removal: orphanRemoval = true Explained](https://howtodoinjava.com/hibernate/orphan-removal-example/).

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
| Cart.java | Parent entity: `@OneToMany` items and `@OneToOne` coupon with `orphanRemoval = true` |
| Item.java | Child entity that owns the `cart_id` foreign key, plus a `@ManyToMany` tag set |
| Coupon.java | One-to-one child of a cart |
| Tag.java | Many-to-many side (no orphan removal) |
| Wishlist.java, WishlistItem.java | The same shape without `orphanRemoval`, for comparison |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` (no persistence.xml) |
| OrphanRemovalDemo.java | Runs each step and prints the SQL |
| OrphanRemovalTest.java | JUnit tests for every behavior described in the article |
