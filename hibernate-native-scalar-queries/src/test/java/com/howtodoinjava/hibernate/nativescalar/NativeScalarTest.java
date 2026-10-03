package com.howtodoinjava.hibernate.nativescalar;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NativeScalarTest {

  private EntityManagerFactory emf;

  @BeforeEach
  void setUp() {
    emf = Database.create(false);     // starts with five named native queries, three without a mapping
    Database.seed(emf);
  }

  @AfterEach
  void tearDown() {
    emf.close();
  }

  @Test
  void factoryStartsWithScalarNamedNativeQueries() {
    assertTrue(emf.isOpen());
  }

  @Test
  void singleColumnReturnsTheValue() {
    Object robins = emf.callInTransaction(em -> em.createNamedQuery("Sighting.countBySpecies")
        .setParameter("species", "Robin")
        .getSingleResult());
    assertEquals(2L, robins);
    assertEquals(Long.class, robins.getClass());
  }

  @Test
  void severalColumnsReturnObjectArrays() {
    List<Object[]> totals = emf.callInTransaction(em ->
        em.createNamedQuery("Sighting.totalsBySpecies", Object[].class).getResultList());
    assertEquals(3, totals.size());
    assertArrayEquals(new Object[] {"Heron", 1L}, totals.get(0));
    assertArrayEquals(new Object[] {"Kingfisher", 1L}, totals.get(1));
    assertArrayEquals(new Object[] {"Robin", 5L}, totals.get(2));
  }

  @Test
  void basicResultClassOnNamedQuery() {
    Object lastSeen = emf.callInTransaction(em -> em.createNamedQuery("Sighting.lastSeen")
        .setParameter("species", "Robin")
        .getSingleResult());
    assertEquals(LocalDate.of(2026, 9, 22), lastSeen);
  }

  @Test
  void scalarTypeConvertsTheValue() {
    Object count = emf.callInTransaction(em ->
        em.createNativeQuery("select count(*) from Sighting", Integer.class).getSingleResult());
    assertEquals(4, count);
    assertEquals(Integer.class, count.getClass());

    List<?> species = emf.callInTransaction(em -> em.createNativeQuery(
        "select distinct species from Sighting order by species", String.class).getResultList());
    assertEquals(List.of("Heron", "Kingfisher", "Robin"), species);
  }

  @Test
  void tupleReadsByAlias() {
    emf.runInTransaction(em -> {
      Tuple tuple = (Tuple) em.createNativeQuery(
              "select species, count, seenOn from Sighting where location = 'Lake Park'", Tuple.class)
          .getSingleResult();
      assertEquals("Robin", tuple.get("species"));
      assertEquals(3, tuple.get("count"));
      assertEquals(LocalDate.of(2026, 9, 20), tuple.get("seenOn", LocalDate.class));
      assertEquals(List.of("SPECIES", "COUNT", "SEENON"),
          tuple.getElements().stream().map(TupleElement::getAlias).toList());
    });
  }

  @Test
  void columnResultMappingWithType() {
    List<Object[]> rows = emf.callInTransaction(em ->
        em.createNamedQuery("Sighting.totalsByLocation", Object[].class).getResultList());
    assertArrayEquals(new Object[] {"Lake Park", 3}, rows.get(0));
    assertArrayEquals(new Object[] {"River Bend", 4}, rows.get(1));
    assertEquals(Integer.class, rows.get(1)[1].getClass());
  }

  @Test
  void javaTypesOfCommonExpressions() {
    Object[] row = (Object[]) emf.callInTransaction(em -> em.createNativeQuery(
            "select count(*), sum(count), avg(count), max(count), max(seenOn), min(species) from Sighting")
        .getSingleResult());
    assertArrayEquals(new Object[] {4L, 7L, 1.75d, 3, LocalDate.of(2026, 9, 25), "Heron"}, row);
  }

  @Test
  void namedUpdateWithoutMapping() {
    int updated = emf.callInTransaction(em -> em.createNamedQuery("Sighting.renameLocation")
        .setParameter("newName", "River Walk")
        .setParameter("oldName", "River Bend")
        .executeUpdate());
    assertEquals(3, updated);
  }

  @Test
  void oldBigIntegerCastFailsAfterUpgrade() {
    Object robins = emf.callInTransaction(em -> em.createNamedQuery("Sighting.countBySpecies")
        .setParameter("species", "Robin")
        .getSingleResult());
    assertThrows(ClassCastException.class, () -> {
      BigInteger old = (BigInteger) robins;
    });
    assertEquals(2L, ((Number) robins).longValue());

    Object all = emf.callInTransaction(em ->
        em.createNativeQuery("select count(*) from Sighting", Long.class).getSingleResult());
    assertEquals(4L, all);
  }

  @Test
  void oldColumnResultWorkaroundStillWorks() {
    Object robins = emf.callInTransaction(em -> em.createNamedQuery("Sighting.countBySpeciesMapped")
        .setParameter("species", "Robin")
        .getSingleResult());
    assertEquals(2L, robins);

    int updated = emf.callInTransaction(em -> em.createNamedQuery("Sighting.renameLocationMapped")
        .setParameter("newName", "River Walk")
        .setParameter("oldName", "River Bend")
        .executeUpdate());
    assertEquals(3, updated);
  }

  @Test
  void jpqlNamedQueryNeedsNoMapping() {
    Long robins = emf.callInTransaction(em -> em.createNamedQuery("Sighting.countJpql", Long.class)
        .setParameter("species", "Robin")
        .getSingleResult());
    assertEquals(2L, robins);
  }
}
