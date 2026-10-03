package com.howtodoinjava.hibernate.criteria;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;

/** Every Criteria query of the article, one method per snippet. */
public final class ListingQueries {

  private ListingQueries() {
  }

  // ---- 1. Basics ----

  public static List<Listing> findAll(EntityManager em) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Listing> query = cb.createQuery(Listing.class);
    Root<Listing> listing = query.from(Listing.class);
    query.select(listing);
    return em.createQuery(query).getResultList();
  }

  public static List<Listing> findActiveUnder(EntityManager em, BigDecimal maxPrice) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Listing> query = cb.createQuery(Listing.class);
    Root<Listing> listing = query.from(Listing.class);
    query.select(listing)
        .where(cb.equal(listing.get("status"), ListingStatus.ACTIVE),
            cb.lessThan(listing.get("price"), maxPrice))
        .orderBy(cb.asc(listing.get("price")));
    return em.createQuery(query).getResultList();
  }

  /** Runs one where condition built by the caller. */
  public static List<Listing> where(EntityManager em, PredicateFactory factory) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Listing> query = cb.createQuery(Listing.class);
    Root<Listing> listing = query.from(Listing.class);
    query.select(listing)
        .where(factory.create(cb, listing))
        .orderBy(cb.asc(listing.get("id")));
    return em.createQuery(query).getResultList();
  }

  @FunctionalInterface
  public interface PredicateFactory {
    Predicate create(CriteriaBuilder cb, Root<Listing> listing);
  }

  public static Predicate statusEquals(CriteriaBuilder cb, Root<Listing> listing) {
    return cb.equal(listing.get("status"), ListingStatus.ACTIVE);
  }

  public static Predicate titleLike(CriteriaBuilder cb, Root<Listing> listing) {
    return cb.like(listing.get("title"), "%Lake%");
  }

  public static Predicate priceBetween(CriteriaBuilder cb, Root<Listing> listing) {
    return cb.between(listing.get("price"), new BigDecimal("250000"), new BigDecimal("400000"));
  }

  public static Predicate statusIn(CriteriaBuilder cb, Root<Listing> listing) {
    return listing.get("status").in(ListingStatus.PENDING, ListingStatus.SOLD);
  }

  public static Predicate noNeighborhood(CriteriaBuilder cb, Root<Listing> listing) {
    return cb.isNull(listing.get("neighborhood"));
  }

  public static Predicate atLeastThreeBedrooms(CriteriaBuilder cb, Root<Listing> listing) {
    return cb.ge(listing.get("bedrooms"), 3);
  }

  public static Predicate listedAfter(CriteriaBuilder cb, Root<Listing> listing) {
    return cb.greaterThan(listing.get("listedOn"), LocalDate.of(2026, 9, 10));
  }

  public static Predicate notSold(CriteriaBuilder cb, Root<Listing> listing) {
    return cb.notEqual(listing.get("status"), ListingStatus.SOLD);
  }

  public static Predicate activeAndRoomyOrCheap(CriteriaBuilder cb, Root<Listing> listing) {
    return cb.and(
        cb.equal(listing.get("status"), ListingStatus.ACTIVE),
        cb.or(
            cb.ge(listing.get("bedrooms"), 2),
            cb.lt(listing.get("price"), 200000)));
  }

  public static Predicate notActive(CriteriaBuilder cb, Root<Listing> listing) {
    return cb.not(cb.equal(listing.get("status"), ListingStatus.ACTIVE));
  }

  /** One query object, run twice with different values for a named parameter. */
  public static List<List<Listing>> sameQueryTwoPrices(EntityManager em, BigDecimal first, BigDecimal second) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Listing> query = cb.createQuery(Listing.class);
    Root<Listing> listing = query.from(Listing.class);
    ParameterExpression<BigDecimal> maxPrice = cb.parameter(BigDecimal.class, "maxPrice");
    query.select(listing)
        .where(cb.le(listing.get("price"), maxPrice))
        .orderBy(cb.asc(listing.get("price")));

    TypedQuery<Listing> typed = em.createQuery(query);
    List<Listing> cheap = typed.setParameter("maxPrice", first).getResultList();
    List<Listing> mid = typed.setParameter("maxPrice", second).getResultList();
    return List.of(cheap, mid);
  }

  // ---- 2. Sorting ----

  public static List<Listing> sortedByBedroomsThenPrice(EntityManager em) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Listing> query = cb.createQuery(Listing.class);
    Root<Listing> listing = query.from(Listing.class);
    query.select(listing)
        .orderBy(cb.desc(listing.get("bedrooms")), cb.asc(listing.get("price")));
    return em.createQuery(query).getResultList();
  }

  // ---- 3. Projections ----

  public static List<String> titles(EntityManager em) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<String> query = cb.createQuery(String.class);
    Root<Listing> listing = query.from(Listing.class);
    query.select(listing.get("title"))
        .orderBy(cb.asc(listing.get("title")));
    return em.createQuery(query).getResultList();
  }

  public static List<Tuple> titleAndPrice(EntityManager em) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Tuple> query = cb.createTupleQuery();
    Root<Listing> listing = query.from(Listing.class);
    query.multiselect(listing.get("title").alias("title"), listing.get("price").alias("price"))
        .where(cb.equal(listing.get("status"), ListingStatus.PENDING))
        .orderBy(cb.asc(listing.get("price")));
    return em.createQuery(query).getResultList();
  }

  public static List<Object[]> titleAndPriceArray(EntityManager em) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
    Root<Listing> listing = query.from(Listing.class);
    query.multiselect(listing.get("title"), listing.get("price"))
        .where(cb.equal(listing.get("status"), ListingStatus.PENDING))
        .orderBy(cb.asc(listing.get("price")));
    return em.createQuery(query).getResultList();
  }

  public static List<ListingSummary> summaries(EntityManager em) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<ListingSummary> query = cb.createQuery(ListingSummary.class);
    Root<Listing> listing = query.from(Listing.class);
    query.select(cb.construct(ListingSummary.class, listing.get("title"), listing.get("price")))
        .where(cb.equal(listing.get("status"), ListingStatus.PENDING))
        .orderBy(cb.asc(listing.get("price")));
    return em.createQuery(query).getResultList();
  }

  // ---- 4. Joins ----

  public static List<Listing> inCity(EntityManager em, String city) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Listing> query = cb.createQuery(Listing.class);
    Root<Listing> listing = query.from(Listing.class);
    Join<Listing, Neighborhood> neighborhood = listing.join("neighborhood");
    query.select(listing)
        .where(cb.equal(neighborhood.get("city"), city))
        .orderBy(cb.asc(listing.get("id")));
    return em.createQuery(query).getResultList();
  }

  public static List<Tuple> titlesWithNeighborhood(EntityManager em) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Tuple> query = cb.createTupleQuery();
    Root<Listing> listing = query.from(Listing.class);
    Join<Listing, Neighborhood> neighborhood = listing.join("neighborhood", JoinType.LEFT);
    query.multiselect(listing.get("title"), neighborhood.get("name"))
        .where(cb.greaterThanOrEqualTo(listing.get("listedOn"), LocalDate.of(2026, 9, 12)))
        .orderBy(cb.asc(listing.get("id")));
    return em.createQuery(query).getResultList();
  }

  public static List<Listing> withAmenity(EntityManager em, String amenityName) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Listing> query = cb.createQuery(Listing.class);
    Root<Listing> listing = query.from(Listing.class);
    Join<Listing, Amenity> amenity = listing.join("amenities");
    query.select(listing)
        .where(cb.equal(amenity.get("name"), amenityName))
        .orderBy(cb.asc(listing.get("id")));
    return em.createQuery(query).getResultList();
  }

  public static List<Listing> withAnyAmenity(EntityManager em, boolean distinct, String... names) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Listing> query = cb.createQuery(Listing.class);
    Root<Listing> listing = query.from(Listing.class);
    Join<Listing, Amenity> amenity = listing.join("amenities");
    query.select(listing)
        .distinct(distinct)
        .where(amenity.get("name").in((Object[]) names))
        .orderBy(cb.asc(listing.get("id")));
    return em.createQuery(query).getResultList();
  }

  public static List<String> titlesWithAnyAmenity(EntityManager em, boolean distinct, String... names) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<String> query = cb.createQuery(String.class);
    Root<Listing> listing = query.from(Listing.class);
    Join<Listing, Amenity> amenity = listing.join("amenities");
    query.select(listing.get("title"))
        .distinct(distinct)
        .where(amenity.get("name").in((Object[]) names))
        .orderBy(cb.asc(listing.get("title")));
    return em.createQuery(query).getResultList();
  }

  /** Without a fetch join: one query for the listings, then one per neighborhood when we read it. */
  public static List<String> labelsWithoutFetch(EntityManager em) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Listing> query = cb.createQuery(Listing.class);
    Root<Listing> listing = query.from(Listing.class);
    query.select(listing)
        .where(cb.isNotNull(listing.get("neighborhood")))
        .orderBy(cb.asc(listing.get("id")));
    return em.createQuery(query).getResultList().stream()
        .map(l -> l.getTitle() + " (" + l.getNeighborhood().getName() + ")")
        .toList();
  }

  /** With a fetch join: the neighborhood is loaded by the same SQL statement. */
  public static List<String> labelsWithFetch(EntityManager em) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Listing> query = cb.createQuery(Listing.class);
    Root<Listing> listing = query.from(Listing.class);
    listing.fetch("neighborhood");
    query.select(listing)
        .orderBy(cb.asc(listing.get("id")));
    return em.createQuery(query).getResultList().stream()
        .map(l -> l.getTitle() + " (" + l.getNeighborhood().getName() + ")")
        .toList();
  }

  public static List<Listing> withAmenitiesFetched(EntityManager em) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Listing> query = cb.createQuery(Listing.class);
    Root<Listing> listing = query.from(Listing.class);
    Fetch<Listing, Amenity> amenities = listing.fetch("amenities", JoinType.LEFT);
    query.select(listing)
        .where(cb.equal(listing.get("status"), ListingStatus.PENDING));
    return em.createQuery(query).getResultList();
  }

  // ---- 5. Group by and having ----

  public static List<NeighborhoodStats> statsByNeighborhood(EntityManager em) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<NeighborhoodStats> query = cb.createQuery(NeighborhoodStats.class);
    Root<Listing> listing = query.from(Listing.class);
    Join<Listing, Neighborhood> neighborhood = listing.join("neighborhood");
    query.select(cb.construct(NeighborhoodStats.class,
            neighborhood.get("name"),
            cb.count(listing),
            cb.avg(listing.get("price"))))
        .groupBy(neighborhood.get("name"))
        .having(cb.gt(cb.avg(listing.get("price")), 300000))
        .orderBy(cb.asc(neighborhood.get("name")));
    return em.createQuery(query).getResultList();
  }

  public static List<Object[]> countByStatus(EntityManager em) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
    Root<Listing> listing = query.from(Listing.class);
    query.multiselect(listing.get("status"), cb.count(listing))
        .groupBy(listing.get("status"))
        .orderBy(cb.asc(listing.get("status")));
    return em.createQuery(query).getResultList();
  }

  // ---- 6. Subqueries ----

  public static List<Listing> pricedAboveAverage(EntityManager em) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Listing> query = cb.createQuery(Listing.class);
    Root<Listing> listing = query.from(Listing.class);

    Subquery<Double> average = query.subquery(Double.class);
    Root<Listing> other = average.from(Listing.class);
    average.select(cb.avg(other.get("price")));

    query.select(listing)
        .where(cb.gt(listing.get("price"), average))
        .orderBy(cb.asc(listing.get("price")));
    return em.createQuery(query).getResultList();
  }

  public static List<Neighborhood> withoutActiveListings(EntityManager em) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Neighborhood> query = cb.createQuery(Neighborhood.class);
    Root<Neighborhood> neighborhood = query.from(Neighborhood.class);

    Subquery<Integer> active = query.subquery(Integer.class);
    Root<Listing> listing = active.from(Listing.class);
    active.select(cb.literal(1))
        .where(cb.equal(listing.get("neighborhood"), neighborhood),
            cb.equal(listing.get("status"), ListingStatus.ACTIVE));

    query.select(neighborhood)
        .where(cb.not(cb.exists(active)))
        .orderBy(cb.asc(neighborhood.get("name")));
    return em.createQuery(query).getResultList();
  }

  // ---- 7. Pagination ----

  public static PageResult<Listing> activePage(EntityManager em, int pageNumber, int pageSize) {
    CriteriaBuilder cb = em.getCriteriaBuilder();

    CriteriaQuery<Listing> query = cb.createQuery(Listing.class);
    Root<Listing> listing = query.from(Listing.class);
    query.select(listing)
        .where(cb.equal(listing.get("status"), ListingStatus.ACTIVE))
        .orderBy(cb.asc(listing.get("price")), cb.asc(listing.get("id")));
    List<Listing> content = em.createQuery(query)
        .setFirstResult((pageNumber - 1) * pageSize)
        .setMaxResults(pageSize)
        .getResultList();

    CriteriaQuery<Long> count = cb.createQuery(Long.class);
    Root<Listing> counted = count.from(Listing.class);
    count.select(cb.count(counted))
        .where(cb.equal(counted.get("status"), ListingStatus.ACTIVE));
    long total = em.createQuery(count).getSingleResult();

    return new PageResult<>(content, pageNumber, pageSize, total);
  }

  // ---- 8. Dynamic filters ----

  public static List<Listing> search(EntityManager em, ListingFilter filter) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Listing> query = cb.createQuery(Listing.class);
    Root<Listing> listing = query.from(Listing.class);

    List<Predicate> predicates = new ArrayList<>();
    if (filter.city() != null) {
      Join<Listing, Neighborhood> neighborhood = listing.join("neighborhood");
      predicates.add(cb.equal(neighborhood.get("city"), filter.city()));
    }
    if (filter.maxPrice() != null) {
      predicates.add(cb.le(listing.get("price"), filter.maxPrice()));
    }
    if (filter.minBedrooms() != null) {
      predicates.add(cb.ge(listing.get("bedrooms"), filter.minBedrooms()));
    }
    if (filter.status() != null) {
      predicates.add(cb.equal(listing.get("status"), filter.status()));
    }
    if (filter.keyword() != null && !filter.keyword().isBlank()) {
      predicates.add(cb.like(cb.lower(listing.get("title")), "%" + filter.keyword().toLowerCase() + "%"));
    }

    query.select(listing)
        .where(predicates)                      // Jakarta Persistence 3.2: where(List<Predicate>)
        .orderBy(cb.asc(listing.get("price")));
    return em.createQuery(query).getResultList();
  }

  // ---- 9. Update and delete ----

  public static int markOldPendingAsSold(EntityManager em, LocalDate before) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaUpdate<Listing> update = cb.createCriteriaUpdate(Listing.class);
    Root<Listing> listing = update.from(Listing.class);
    update.set(listing.get("status"), ListingStatus.SOLD)
        .where(cb.equal(listing.get("status"), ListingStatus.PENDING),
            cb.lessThan(listing.get("listedOn"), before));
    return em.createQuery(update).executeUpdate();
  }

  public static int cutPrice(EntityManager em, BigDecimal amount, int minDaysListed, LocalDate today) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaUpdate<Listing> update = cb.createCriteriaUpdate(Listing.class);
    Root<Listing> listing = update.from(Listing.class);
    Path<BigDecimal> price = listing.get("price");
    update.set(price, cb.diff(price, amount))
        .where(cb.equal(listing.get("status"), ListingStatus.ACTIVE),
            cb.lessThanOrEqualTo(listing.get("listedOn"), today.minusDays(minDaysListed)));
    return em.createQuery(update).executeUpdate();
  }

  public static int deleteByStatus(EntityManager em, ListingStatus status) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaDelete<Listing> delete = cb.createCriteriaDelete(Listing.class);
    Root<Listing> listing = delete.from(Listing.class);
    delete.where(cb.equal(listing.get("status"), status));
    return em.createQuery(delete).executeUpdate();
  }

  public static int deleteNeighborhood(EntityManager em, String name) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaDelete<Neighborhood> delete = cb.createCriteriaDelete(Neighborhood.class);
    Root<Neighborhood> neighborhood = delete.from(Neighborhood.class);
    delete.where(cb.equal(neighborhood.get("name"), name));
    return em.createQuery(delete).executeUpdate();
  }

  // ---- 10. Static metamodel ----

  public static List<Listing> inCityTypeSafe(EntityManager em, String city, BigDecimal maxPrice) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Listing> query = cb.createQuery(Listing.class);
    Root<Listing> listing = query.from(Listing.class);
    Join<Listing, Neighborhood> neighborhood = listing.join(Listing_.neighborhood);
    query.select(listing)
        .where(cb.equal(neighborhood.get(Neighborhood_.city), city),
            cb.le(listing.get(Listing_.price), maxPrice))
        .orderBy(cb.asc(listing.get(Listing_.price)));
    return em.createQuery(query).getResultList();
  }

  public static List<Listing> misspelledAttribute(EntityManager em) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Listing> query = cb.createQuery(Listing.class);
    Root<Listing> listing = query.from(Listing.class);
    query.select(listing).where(cb.lt(listing.get("prise"), 300000));
    return em.createQuery(query).getResultList();
  }

  // ---- 11. HibernateCriteriaBuilder ----

  public static List<Listing> titleContainsIgnoreCase(EntityManager em, String text) {
    HibernateCriteriaBuilder cb = em.unwrap(Session.class).getCriteriaBuilder();
    JpaCriteriaQuery<Listing> query = cb.createQuery(Listing.class);
    JpaRoot<Listing> listing = query.from(Listing.class);
    query.select(listing)
        .where(cb.ilike(listing.get(Listing_.title), "%" + text + "%"));
    return em.createQuery(query).getResultList();
  }

  public static long countFromQuery(EntityManager em) {
    HibernateCriteriaBuilder cb = em.unwrap(Session.class).getCriteriaBuilder();
    JpaCriteriaQuery<Listing> query = cb.createQuery(Listing.class);
    JpaRoot<Listing> listing = query.from(Listing.class);
    query.select(listing)
        .where(cb.ge(listing.get(Listing_.bedrooms), 2))
        .orderBy(cb.asc(listing.get(Listing_.price)));
    return em.createQuery(query.createCountQuery()).getSingleResult();
  }

  public static List<Listing> fromHqlThenRefine(EntityManager em) {
    HibernateCriteriaBuilder cb = em.unwrap(Session.class).getCriteriaBuilder();
    JpaCriteriaQuery<Listing> query = cb.createQuery("from Listing l where l.bedrooms >= 2", Listing.class);
    JpaRoot<?> listing = query.getRoot(0, Listing.class);
    query.where(query.getRestriction(), cb.equal(listing.get("status"), ListingStatus.PENDING));
    return em.createQuery(query).getResultList();
  }
}
