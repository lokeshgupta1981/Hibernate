package com.howtodoinjava.hibernate.sorting;

import static com.howtodoinjava.hibernate.sorting.TrailQueries.names;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.function.Function;
import jakarta.persistence.EntityManager;
import org.hibernate.collection.spi.PersistentSortedSet;
import org.hibernate.query.Order;
import org.hibernate.query.sqm.PathElementException;
import org.hibernate.query.specification.SelectionSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SortingTest {

  private EntityManagerFactory emf;
  private Long eagleId;

  @BeforeEach
  void setUp() {
    emf = Database.create(false);
    eagleId = Database.load(emf);
  }

  @AfterEach
  void tearDown() {
    emf.close();
  }

  private List<String> hql(String query) {
    return emf.callInTransaction(em -> names(TrailQueries.hql(em, query)));
  }

  private <T> T call(Function<EntityManager, T> work) {
    return emf.callInTransaction(work);
  }

  private List<String> reviews(Function<Trail, Object> collection) {
    return emf.callInTransaction(em -> {
      Object c = collection.apply(em.find(Trail.class, eagleId));
      return ((java.util.Collection<?>) c).stream().map(Object::toString).toList();
    });
  }

  // ---------- JPQL ----------

  @Test
  void ascendingIsTheDefault() {
    assertEquals(List.of("Lake Loop", "bear creek", "Pine Ridge", "Eagle Peak", "Canyon Rim"),
        hql("from Trail t order by t.lengthKm"));
  }

  @Test
  void descending() {
    assertEquals(List.of("Canyon Rim", "Eagle Peak", "Pine Ridge", "bear creek", "Lake Loop"),
        hql("from Trail t order by t.lengthKm desc"));
  }

  @Test
  void twoColumnsAndH2PutsNullsFirstForAsc() {
    assertEquals(List.of("Pine Ridge", "Eagle Peak", "Lake Loop", "Canyon Rim", "bear creek"),
        hql("from Trail t order by t.region, t.lengthKm desc"));
  }

  @Test
  void nullsLastAndNullsFirst() {
    assertEquals(List.of("Eagle Peak", "Lake Loop", "Canyon Rim", "bear creek", "Pine Ridge"),
        hql("from Trail t order by t.region nulls last, t.name"));
    assertEquals(List.of("Pine Ridge", "Canyon Rim", "bear creek", "Eagle Peak", "Lake Loop"),
        hql("from Trail t order by t.region desc nulls first, t.name"));
  }

  @Test
  void plainOrderIsCaseSensitiveLowerIsNot() {
    assertEquals(List.of("Canyon Rim", "Eagle Peak", "Lake Loop", "Pine Ridge", "bear creek"),
        hql("from Trail t order by t.name"));
    assertEquals(List.of("bear creek", "Canyon Rim", "Eagle Peak", "Lake Loop", "Pine Ridge"),
        hql("from Trail t order by lower(t.name)"));
  }

  @Test
  void customOrderWithCase() {
    assertEquals(List.of("Lake Loop", "bear creek", "Pine Ridge", "Canyon Rim", "Eagle Peak"),
        hql("from Trail t order by case t.difficulty when 'beginner' then 1 when 'intermediate' then 2 else 3 end, t.name"));
  }

  @Test
  void orderByAssociatedEntityAttribute() {
    List<String> result = call(em -> em
        .createQuery("from TrailReview r order by r.trail.name, r.rating desc", TrailReview.class)
        .getResultList().stream().map(Object::toString).toList());
    assertEquals(List.of("Lokesh 5", "alex 5", "Maria 3"), result);
  }

  // ---------- Criteria API ----------

  @Test
  void criteriaNullsLastThenDesc() {
    assertEquals(List.of("Eagle Peak", "Lake Loop", "Canyon Rim", "bear creek", "Pine Ridge"),
        call(em -> names(TrailQueries.criteriaByRegionThenLength(em))));
  }

  @Test
  void criteriaLower() {
    assertEquals(List.of("bear creek", "Canyon Rim", "Eagle Peak", "Lake Loop", "Pine Ridge"),
        call(em -> names(TrailQueries.criteriaIgnoringCase(em))));
  }

  // ---------- Dynamic sorting ----------

  @Test
  void allowListAcceptsKnownField() {
    assertEquals(List.of("Canyon Rim", "Eagle Peak", "Pine Ridge", "bear creek", "Lake Loop"),
        call(em -> names(TrailQueries.findAll(em, "length", true))));
  }

  @Test
  void allowListRejectsUnknownField() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> call(em -> TrailQueries.findAll(em, "name; drop table Trail", false)));
    assertEquals("Cannot sort by: name; drop table Trail", e.getMessage());
  }

  @Test
  void selectionSpecificationSort() {
    assertEquals(List.of("Lake Loop", "bear creek", "Eagle Peak", "Canyon Rim", "Pine Ridge"),
        call(em -> names(TrailQueries.findAllSpec(em, "difficulty", false))));
    assertEquals(List.of("Lake Loop", "bear creek", "Pine Ridge", "Eagle Peak", "Canyon Rim"),
        call(em -> names(TrailQueries.findAllSpec(em, "length", false))));
  }

  @Test
  void orderIgnoringCaseAndNullsLast() {
    assertEquals(List.of("bear creek", "Canyon Rim", "Eagle Peak", "Lake Loop", "Pine Ridge"),
        call(em -> names(TrailQueries.specIgnoringCase(em))));
    assertEquals(List.of("Eagle Peak", "Lake Loop", "Canyon Rim", "bear creek", "Pine Ridge"),
        call(em -> names(TrailQueries.specNullsLast(em))));
  }

  @Test
  void orderWithUnknownAttributeFails() {
    PathElementException e = assertThrows(PathElementException.class, () -> call(em ->
        SelectionSpecification.create(Trail.class, "from Trail")
            .sort(Order.asc(Trail.class, "nope"))
            .createQuery(em).getResultList()));
    assertTrue(e.getMessage().startsWith("Could not resolve attribute 'nope'"));
  }

  @Test
  void concatenationRunsInjectedSubquery() {
    // the order changes with the number of reviews by authors starting with 'L'
    assertEquals(List.of("Eagle Peak", "Lake Loop", "Pine Ridge", "bear creek", "Canyon Rim"),
        call(em -> names(TrailQueries.unsafe(em,
            "id * (select count(*) from TrailReview r where r.author like 'L%')"))));
  }

  @Test
  void concatenationWithSemicolonIsASyntaxError() {
    assertThrows(IllegalArgumentException.class,
        () -> call(em -> TrailQueries.unsafe(em, "name; drop table Trail")));
  }

  // ---------- Collections ----------

  @Test
  void orderByAnnotationSortsInDatabase() {
    assertEquals(List.of("Lokesh 5", "alex 5", "Maria 3"), reviews(Trail::getReviews));
  }

  @Test
  void sortComparatorSortsInMemory() {
    assertEquals(List.of("Lokesh 5", "alex 5", "Maria 3"), reviews(Trail::getSortedReviews));
    Object type = call(em -> em.find(Trail.class, eagleId).getSortedReviews());
    assertInstanceOf(PersistentSortedSet.class, type);
  }

  @Test
  void sqlOrderUsesSqlFragment() {
    assertEquals(List.of("alex 5", "Lokesh 5", "Maria 3"), reviews(Trail::getReviewsByAuthor));
  }

  @Test
  void orderColumnKeepsInsertionOrder() {
    assertEquals(List.of("Parking", "Bridge", "Hut", "Summit"), reviews(Trail::getWaypoints));
  }

  @Test
  void sortNaturalUsesCompareTo() {
    assertEquals(List.of("Forest", "lake", "views"), reviews(Trail::getTags));
  }

  @Test
  void removingWaypointRewritesPositions() {
    emf.runInTransaction(em -> em.find(Trail.class, eagleId).getWaypoints().remove("Bridge"));
    assertEquals(List.of("Parking", "Hut", "Summit"), reviews(Trail::getWaypoints));
    List<Integer> positions = call(em -> em.createNativeQuery(
        "select position from Trail_waypoints order by position", Integer.class).getResultList());
    assertEquals(List.of(0, 1, 2), positions);
  }

  @Test
  void joinFetchKeepsOrderBy() {
    List<String> result = call(em -> em
        .createQuery("from Trail t join fetch t.reviews where t.id = :id", Trail.class)
        .setParameter("id", eagleId)
        .getSingleResult().getReviews().stream().map(Object::toString).toList());
    assertEquals(List.of("Lokesh 5", "alex 5", "Maria 3"), result);
  }

  @Test
  void newElementIsAppendedToOrderByListUntilReload() {
    List<String> inMemory = call(em -> {
      Trail eagle = em.find(Trail.class, eagleId);
      eagle.getReviews().size();
      TrailReview anna = new TrailReview("Anna", 4);
      eagle.addReview(anna);
      em.persist(anna);
      return eagle.getReviews().stream().map(Object::toString).toList();
    });
    assertEquals(List.of("Lokesh 5", "alex 5", "Maria 3", "Anna 4"), inMemory);
    assertEquals(List.of("Lokesh 5", "alex 5", "Anna 4", "Maria 3"), reviews(Trail::getReviews));
  }

  @Test
  void newElementIsSortedAtOnceInSortedSet() {
    List<String> inMemory = call(em -> {
      Trail eagle = em.find(Trail.class, eagleId);
      eagle.getSortedReviews().add(new TrailReview("Anna", 4));
      return eagle.getSortedReviews().stream().map(Object::toString).toList();
    });
    assertEquals(List.of("Lokesh 5", "alex 5", "Anna 4", "Maria 3"), inMemory);
  }

  @Test
  void comparatorWithoutTieBreakerDropsElements() {
    java.util.SortedSet<TrailReview> byRatingOnly =
        new java.util.TreeSet<>(java.util.Comparator.comparingInt(TrailReview::getRating));
    byRatingOnly.add(new TrailReview("Lokesh", 5));
    byRatingOnly.add(new TrailReview("alex", 5));
    assertEquals(1, byRatingOnly.size());
  }

  // ---------- Global null ordering ----------

  @Test
  void defaultNullOrderingSetting() {
    emf.close();
    emf = Database.create(false, "last");
    Database.load(emf);
    assertEquals(List.of("Eagle Peak", "Lake Loop", "Canyon Rim", "bear creek", "Pine Ridge"),
        hql("from Trail t order by t.region, t.name"));
  }

  @Test
  void sortedSetTypeIsPersistentSortedSet() {
    Object type = call(em -> em.find(Trail.class, eagleId).getTags());
    assertInstanceOf(PersistentSortedSet.class, type);
  }
}
