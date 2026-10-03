package com.howtodoinjava.hibernate.criteria;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Tuple;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

public class CriteriaDemo {

  public static void main(String[] args) {
    try (EntityManagerFactory emf = Database.create(true)) {
      Database.seed(emf);

      step("1. All listings");
      emf.runInTransaction(em -> print(ListingQueries.findAll(em)));

      step("2. Active listings under 300000, cheapest first");
      emf.runInTransaction(em -> print(ListingQueries.findActiveUnder(em, new BigDecimal("300000"))));

      step("3. equal: status = ACTIVE");
      emf.runInTransaction(em -> print(ListingQueries.where(em, ListingQueries::statusEquals)));
      step("4. like: title like %Lake%");
      emf.runInTransaction(em -> print(ListingQueries.where(em, ListingQueries::titleLike)));
      step("5. between: price between 250000 and 400000");
      emf.runInTransaction(em -> print(ListingQueries.where(em, ListingQueries::priceBetween)));
      step("6. in: status in (PENDING, SOLD)");
      emf.runInTransaction(em -> print(ListingQueries.where(em, ListingQueries::statusIn)));
      step("7. isNull: no neighborhood");
      emf.runInTransaction(em -> print(ListingQueries.where(em, ListingQueries::noNeighborhood)));
      step("8. ge: bedrooms >= 3");
      emf.runInTransaction(em -> print(ListingQueries.where(em, ListingQueries::atLeastThreeBedrooms)));
      step("9. greaterThan: listedOn > 2026-09-10");
      emf.runInTransaction(em -> print(ListingQueries.where(em, ListingQueries::listedAfter)));
      step("10. notEqual: status <> SOLD");
      emf.runInTransaction(em -> print(ListingQueries.where(em, ListingQueries::notSold)));
      step("11. and/or: ACTIVE and (bedrooms >= 2 or price < 200000)");
      emf.runInTransaction(em -> print(ListingQueries.where(em, ListingQueries::activeAndRoomyOrCheap)));
      step("12. not: not ACTIVE");
      emf.runInTransaction(em -> print(ListingQueries.where(em, ListingQueries::notActive)));

      step("12a. ParameterExpression: same query, maxPrice 200000 then 300000");
      emf.runInTransaction(em -> print(ListingQueries.sameQueryTwoPrices(em, new BigDecimal("200000"),
          new BigDecimal("300000"))));

      step("13. order by bedrooms desc, price asc");
      emf.runInTransaction(em -> print(ListingQueries.sortedByBedroomsThenPrice(em)));

      step("14. One attribute: titles");
      emf.runInTransaction(em -> print(ListingQueries.titles(em)));
      step("15. Tuple: title and price of PENDING listings");
      emf.runInTransaction(em -> {
        for (Tuple row : ListingQueries.titleAndPrice(em)) {
          System.out.println("  " + row.get("title", String.class) + " = " + row.get("price", BigDecimal.class));
        }
      });
      step("16. Object[]: title and price");
      emf.runInTransaction(em -> ListingQueries.titleAndPriceArray(em)
          .forEach(row -> System.out.println("  " + Arrays.toString(row))));
      step("17. construct(): ListingSummary records");
      emf.runInTransaction(em -> print(ListingQueries.summaries(em)));

      step("18. Inner join: listings in Austin");
      emf.runInTransaction(em -> print(ListingQueries.inCity(em, "Austin")));
      step("19. Left join: title and neighborhood name, listed on or after 2026-09-12");
      emf.runInTransaction(em -> ListingQueries.titlesWithNeighborhood(em)
          .forEach(row -> System.out.println("  " + row.get(0) + " -> " + row.get(1))));
      step("20. Join a many-to-many: listings with a Pool");
      emf.runInTransaction(em -> print(ListingQueries.withAmenity(em, "Pool")));
      step("21. Join without distinct: Garage or Garden");
      emf.runInTransaction(em -> print(ListingQueries.withAnyAmenity(em, false, "Garage", "Garden")));
      step("22. Join with distinct: Garage or Garden");
      emf.runInTransaction(em -> print(ListingQueries.withAnyAmenity(em, true, "Garage", "Garden")));
      step("22a. Titles without distinct: Garage or Garden");
      emf.runInTransaction(em -> print(ListingQueries.titlesWithAnyAmenity(em, false, "Garage", "Garden")));
      step("22b. Titles with distinct: Garage or Garden");
      emf.runInTransaction(em -> print(ListingQueries.titlesWithAnyAmenity(em, true, "Garage", "Garden")));
      step("23. Without fetch join (N+1)");
      emf.runInTransaction(em -> print(ListingQueries.labelsWithoutFetch(em)));
      step("24. With fetch join");
      emf.runInTransaction(em -> print(ListingQueries.labelsWithFetch(em)));
      step("25. Fetch join of a collection: PENDING listings with amenities");
      emf.runInTransaction(em -> ListingQueries.withAmenitiesFetched(em)
          .forEach(l -> System.out.println("  " + l + " " + l.getAmenities().stream().map(Amenity::getName).sorted().toList())));

      step("26. group by neighborhood having avg(price) > 300000");
      emf.runInTransaction(em -> print(ListingQueries.statsByNeighborhood(em)));
      step("27. group by status");
      emf.runInTransaction(em -> ListingQueries.countByStatus(em)
          .forEach(row -> System.out.println("  " + Arrays.toString(row))));

      step("28. Subquery: price above the average price");
      emf.runInTransaction(em -> print(ListingQueries.pricedAboveAverage(em)));
      step("29. not exists: neighborhoods without an ACTIVE listing");
      emf.runInTransaction(em -> print(ListingQueries.withoutActiveListings(em)));

      step("30. Page 2 of active listings, 2 per page, with count query");
      emf.runInTransaction(em -> {
        PageResult<Listing> page = ListingQueries.activePage(em, 2, 2);
        System.out.println("  content = " + page.content() + ", total = " + page.totalElements()
            + ", totalPages = " + page.totalPages());
      });

      step("31. Dynamic filter: empty");
      emf.runInTransaction(em -> print(ListingQueries.search(em, ListingFilter.empty())));
      step("32. Dynamic filter: city Denver, at least 2 bedrooms");
      emf.runInTransaction(em -> print(ListingQueries.search(em, new ListingFilter("Denver", null, 2, null, null))));
      step("33. Dynamic filter: max price 300000, ACTIVE, keyword 'garden'");
      emf.runInTransaction(em -> print(ListingQueries.search(em,
          new ListingFilter(null, new BigDecimal("300000"), null, ListingStatus.ACTIVE, "garden"))));

      step("34. Metamodel: Austin listings up to 400000");
      emf.runInTransaction(em -> print(ListingQueries.inCityTypeSafe(em, "Austin", new BigDecimal("400000"))));
      step("35. Misspelled attribute name");
      try {
        emf.runInTransaction(em -> ListingQueries.misspelledAttribute(em));
      } catch (RuntimeException e) {
        System.out.println("  " + e.getClass().getName() + ": " + e.getMessage());
      }

      step("36. HibernateCriteriaBuilder.ilike(): '%LAKE%'");
      emf.runInTransaction(em -> print(ListingQueries.titleContainsIgnoreCase(em, "LAKE")));
      step("37. createCountQuery()");
      emf.runInTransaction(em -> System.out.println("  count = " + ListingQueries.countFromQuery(em)));
      step("38. Criteria query from HQL, refined in code");
      emf.runInTransaction(em -> print(ListingQueries.fromHqlThenRefine(em)));

      step("39. CriteriaDelete: delete SOLD listings");
      emf.runInTransaction(em -> System.out.println("  deleted = " + ListingQueries.deleteByStatus(em, ListingStatus.SOLD)));
      step("40. CriteriaUpdate: PENDING listed before 2026-09-10 becomes SOLD");
      emf.runInTransaction(em -> System.out.println("  updated = "
          + ListingQueries.markOldPendingAsSold(em, LocalDate.of(2026, 9, 10))));
      step("41. CriteriaUpdate with an expression: price - 10000 for ACTIVE listings listed 21+ days ago");
      emf.runInTransaction(em -> System.out.println("  updated = "
          + ListingQueries.cutPrice(em, new BigDecimal("10000"), 21, LocalDate.of(2026, 10, 3))));
      emf.runInTransaction(em -> print(ListingQueries.findActiveUnder(em, new BigDecimal("1000000"))
          .stream().map(l -> l.getTitle() + "=" + l.getPrice()).toList()));
      step("42. CriteriaDelete of a neighborhood that listings still reference");
      try {
        emf.runInTransaction(em -> ListingQueries.deleteNeighborhood(em, "Riverside"));
      } catch (RuntimeException e) {
        System.out.println("  " + e.getClass().getName() + ": " + e.getMessage());
      }
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("=== " + title);
  }

  private static void print(Object result) {
    System.out.println("  " + result);
  }
}
