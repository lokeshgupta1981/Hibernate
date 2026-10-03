package com.howtodoinjava.hibernate.nativeselect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.hibernate.InstantiationException;
import org.hibernate.Session;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.loader.NonUniqueDiscoveredSqlAliasException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NativeSelectTest {

  private EntityManagerFactory emf;

  @BeforeAll
  void setUp() {
    emf = Database.create(false);
    Database.insertSampleData(emf);
  }

  @AfterAll
  void tearDown() {
    emf.close();
  }

  private static List<String> names(List<?> produce) {
    return produce.stream().map(p -> ((Produce) p).getName()).toList();
  }

  @Test
  void entityResultIsManaged() {
    emf.runInTransaction(em -> {
      List<?> fruit = em.createNativeQuery(
              "select * from Produce where category = ?1 order by pricePerKg", Produce.class)
          .setParameter(1, "fruit")
          .getResultList();
      assertEquals(List.of("Apples", "Pears", "Strawberries"), names(fruit));
      assertTrue(em.contains(fruit.get(0)));
    });
  }

  @Test
  void nativeEntityIsSameInstanceAsFind() {
    emf.runInTransaction(em -> {
      Produce found = em.find(Produce.class, 1L);
      Object fromSql = em.createNativeQuery("select * from Produce where id = 1", Produce.class)
          .getSingleResult();
      assertSame(found, fromSql);
    });
  }

  @Test
  void changedEntityIsUpdatedAtCommit() {
    emf.runInTransaction(em -> {
      Produce pears = (Produce) em.createNativeQuery("select * from Produce where name = :name", Produce.class)
          .setParameter("name", "Pears")
          .getSingleResult();
      pears.setPricePerKg(new BigDecimal("3.20"));
    });
    Object price = emf.callInTransaction(em ->
        em.createNativeQuery("select pricePerKg from Produce where name = 'Pears'").getSingleResult());
    assertEquals(new BigDecimal("3.20"), price);
    emf.runInTransaction(em -> em.createNativeQuery(
        "update Produce set pricePerKg = 3.00 where name = 'Pears'").executeUpdate());
  }

  @Test
  void missingColumnFailsForEntityResult() {
    emf.runInTransaction(em -> {
      SQLGrammarException e = assertThrows(SQLGrammarException.class,
          () -> em.createNativeQuery("select id, name from Produce", Produce.class).getResultList());
      assertTrue(e.getMessage().startsWith("Unable to find column position by name: category"));
    });
  }

  @Test
  void jdbcStylePositionalParameters() {
    emf.runInTransaction(em -> assertEquals(List.of("Apples"), names(em.createNativeQuery(
            "select * from Produce where category = ? and pricePerKg < ?", Produce.class)
        .setParameter(1, "fruit")
        .setParameter(2, new BigDecimal("3.00"))
        .getResultList())));
  }

  @Test
  void namedParameters() {
    emf.runInTransaction(em -> assertEquals(List.of("Apples", "Carrots", "Pears", "Tomatoes"),
        names(em.createNativeQuery(
                "select * from Produce where pricePerKg between :min and :max order by name", Produce.class)
            .setParameter("min", new BigDecimal("1.00"))
            .setParameter("max", new BigDecimal("3.00"))
            .getResultList())));
  }

  @Test
  void listParameterExpandsToOnePlaceholderPerElement() {
    emf.runInTransaction(em -> assertEquals(List.of("Carrots", "Pears"), names(em.createNativeQuery(
            "select * from Produce where name in (:names) order by name", Produce.class)
        .setParameter("names", List.of("Pears", "Carrots"))
        .getResultList())));
  }

  @Test
  void namedNativeQueryWithResultClass() {
    emf.runInTransaction(em -> assertEquals(List.of("Carrots", "Potatoes", "Tomatoes"),
        names(em.createNamedQuery("Produce.findByCategory", Produce.class)
            .setParameter("category", "vegetable")
            .getResultList())));
  }

  @Test
  void namedNativeQueryWithWrongClass() {
    emf.runInTransaction(em -> {
      IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
          () -> em.createNamedQuery("Produce.findByCategory", Farm.class));
      assertTrue(e.getMessage().startsWith("Type specified for TypedQuery"));
    });
  }

  @Test
  void singleColumnScalars() {
    emf.runInTransaction(em -> {
      assertEquals(List.of("Apples", "Carrots", "Pears", "Potatoes", "Strawberries", "Tomatoes"),
          em.createNativeQuery("select name from Produce order by name").getResultList());
      assertEquals(6L, em.createNativeQuery("select count(*) from Produce").getSingleResult());
      assertEquals(new BigDecimal("6.00"),
          em.createNativeQuery("select max(pricePerKg) from Produce").getSingleResult());
      assertEquals(6, em.createNativeQuery("select count(*) from Produce", Integer.class).getSingleResult());
    });
  }

  @Test
  void objectArrayColumnTypes() {
    emf.runInTransaction(em -> {
      Object[] row = (Object[]) em.createNativeQuery(
              "select id, name, pricePerKg, harvestedOn, farm_id from Produce where name = 'Apples'")
          .getSingleResult();
      assertInstanceOf(Long.class, row[0]);
      assertEquals("Apples", row[1]);
      assertEquals(new BigDecimal("2.50"), row[2]);
      assertEquals(LocalDate.of(2026, 9, 28), row[3]);
      assertInstanceOf(Long.class, row[4]);
    });
  }

  @Test
  void moreColumnTypes() {
    emf.runInTransaction(em -> {
      Object[] row = (Object[]) em.createNativeQuery(
              "select cast(1 as int), true, cast(1.5 as double), cast(current_timestamp as timestamp), "
                  + "cast('10:15' as time), current_timestamp, avg(pricePerKg) from Produce")
          .getSingleResult();
      assertInstanceOf(Integer.class, row[0]);
      assertInstanceOf(Boolean.class, row[1]);
      assertInstanceOf(Double.class, row[2]);
      assertInstanceOf(LocalDateTime.class, row[3]);
      assertInstanceOf(LocalTime.class, row[4]);
      assertInstanceOf(OffsetDateTime.class, row[5]);
      assertInstanceOf(BigDecimal.class, row[6]);
    });
  }

  @Test
  void tupleByAliasIgnoresCase() {
    emf.runInTransaction(em -> {
      Tuple tuple = (Tuple) em.createNativeQuery(
              "select name, pricePerKg as price from Produce where name = 'Apples'", Tuple.class)
          .getSingleResult();
      assertEquals("Apples", tuple.get("name"));
      assertEquals(new BigDecimal("2.50"), tuple.get("price", BigDecimal.class));
      assertEquals(List.of("NAME", "PRICE"),
          tuple.getElements().stream().map(TupleElement::getAlias).toList());
    });
  }

  @Test
  void adHocQueryMapsToRecord() {
    emf.runInTransaction(em -> assertEquals(List.of(
            new ProducePrice("Potatoes", new BigDecimal("0.90")),
            new ProducePrice("Carrots", new BigDecimal("1.20")),
            new ProducePrice("Tomatoes", new BigDecimal("2.80"))),
        em.createNativeQuery("select name, pricePerKg from Produce where category = 'vegetable' "
            + "order by pricePerKg", ProducePrice.class).getResultList()));
  }

  @Test
  void recordColumnsMustMatchConstructorOrder() {
    emf.runInTransaction(em -> {
      InstantiationException e = assertThrows(InstantiationException.class,
          () -> em.createNativeQuery("select pricePerKg, name from Produce", ProducePrice.class).getResultList());
      assertTrue(e.getMessage().contains(
          "expected 'ProducePrice(String, BigDecimal)' but found 'ProducePrice(BigDecimal, String)'"));
    });
  }

  @Test
  void namedNativeQueryWithRecordResultClassFails() {
    emf.runInTransaction(em -> {
      RuntimeException e = assertThrows(RuntimeException.class,
          () -> em.createNamedQuery("Produce.pricesAsRecord", ProducePrice.class).getResultList());
      assertEquals("org.hibernate.type.descriptor.java.spi.JdbcTypeRecommendationException", e.getClass().getName());
      assertEquals("Could not determine recommended JdbcType for Java type "
          + "'com.howtodoinjava.hibernate.nativeselect.ProducePrice'", e.getMessage());
    });
  }

  @Test
  void constructorResultMapping() {
    List<CategoryPrice> expected = List.of(
        new CategoryPrice("fruit", 3L, new BigDecimal("2.50")),
        new CategoryPrice("vegetable", 3L, new BigDecimal("0.90")));
    emf.runInTransaction(em -> {
      assertEquals(expected, em.createNamedQuery("Produce.categoryPrices", CategoryPrice.class).getResultList());
      assertEquals(expected, em.createNativeQuery("select category, count(*) as items, min(pricePerKg) as cheapest "
          + "from Produce group by category order by category", "CategoryPriceMapping").getResultList());
    });
  }

  @Test
  void tupleTransformerMapsToRecord() {
    emf.runInTransaction(em -> assertEquals(List.of(
            new ProducePrice("Carrots", new BigDecimal("1.20")),
            new ProducePrice("Potatoes", new BigDecimal("0.90"))),
        em.unwrap(Session.class)
            .createNativeQuery("select name, pricePerKg from Produce where pricePerKg < 2", Object[].class)
            .setTupleTransformer((tuple, aliases) -> new ProducePrice((String) tuple[0], (BigDecimal) tuple[1]))
            .getResultList()));
  }

  @Test
  void addScalarConvertsType() {
    emf.runInTransaction(em -> {
      Object[] row = em.unwrap(Session.class)
          .createNativeQuery("select name, pricePerKg from Produce where name = 'Apples'", Object[].class)
          .addScalar("name", String.class)
          .addScalar("pricePerKg", Double.class)
          .getSingleResult();
      assertEquals(2.5d, row[1]);
    });
  }

  @Test
  void twoEntitiesPerRowWithEntityResult() {
    emf.runInTransaction(em -> {
      List<Object[]> rows = em.createNamedQuery("Produce.withFarmByRegion", Object[].class)
          .setParameter("region", "North")
          .getResultList();
      assertEquals(3, rows.size());
      Produce apples = (Produce) rows.get(0)[0];
      Farm farm = (Farm) rows.get(0)[1];
      assertEquals("Apples", apples.getName());
      assertEquals("Green Valley", farm.getName());
      assertEquals("North", farm.getRegion());
      assertSame(farm, apples.getFarm());
      assertTrue(em.contains(farm));
    });
  }

  @Test
  void joinWithoutMappingFails() {
    emf.runInTransaction(em -> {
      NonUniqueDiscoveredSqlAliasException e = assertThrows(NonUniqueDiscoveredSqlAliasException.class,
          () -> em.createNativeQuery("select * from Produce p join Farm f on f.id = p.farm_id", Produce.class)
              .getResultList());
      assertEquals("Encountered a duplicated sql alias [ID] during auto-discovery of a native-sql query",
          e.getMessage());
    });
  }

  @Test
  void hibernateAddEntityWithAliasPlaceholders() {
    emf.runInTransaction(em -> {
      List<Object[]> rows = em.unwrap(Session.class)
          .createNativeQuery("select {p.*}, {f.*} from Produce p join Farm f on f.id = p.farm_id "
              + "where f.region = :region order by p.name", Object[].class)
          .addEntity("p", Produce.class)
          .addEntity("f", Farm.class)
          .setParameter("region", "South")
          .getResultList();
      assertEquals(List.of("Potatoes", "Strawberries", "Tomatoes"),
          rows.stream().map(r -> ((Produce) r[0]).getName()).toList());
      assertEquals("Sunny Acres", ((Farm) rows.get(0)[1]).getName());
    });
  }

  @Test
  void pagination() {
    emf.runInTransaction(em -> assertEquals(List.of("Pears", "Potatoes"), names(
        em.createNativeQuery("select * from Produce order by name", Produce.class)
            .setFirstResult(2)
            .setMaxResults(2)
            .getResultList())));
  }

  @Test
  void pendingChangesAreFlushedBeforeNativeQuery() {
    emf.runInTransaction(em -> {
      em.persist(new Produce("Plums", "fruit", "4.00", null, null));
      assertEquals(7L, em.createNativeQuery("select count(*) from Produce").getSingleResult());
      em.getTransaction().setRollbackOnly();
    });
    emf.runInTransaction(em ->
        assertEquals(6L, em.createNativeQuery("select count(*) from Produce").getSingleResult()));
  }

  @Test
  void noResultAndSingleResultOrNull() {
    emf.runInTransaction(em -> {
      NoResultException e = assertThrows(NoResultException.class, () -> em.createNativeQuery(
          "select * from Produce where name = 'Kiwi'", Produce.class).getSingleResult());
      assertEquals("No result found for query [select * from Produce where name = 'Kiwi']", e.getMessage());
      assertNull(em.createNativeQuery("select * from Produce where name = 'Kiwi'", Produce.class)
          .getSingleResultOrNull());
    });
  }

  @Test
  void typedQueryFromHibernateSession() {
    emf.runInTransaction(em -> {
      Produce apples = em.unwrap(Session.class)
          .createNativeQuery("select * from Produce where name = :name", Produce.class)
          .setParameter("name", "Apples")
          .getSingleResult();
      assertEquals(new BigDecimal("2.50"), apples.getPricePerKg());
    });
  }

  @Test
  void wrongTableNameFailsWhenQueryRuns() {
    emf.runInTransaction(em -> {
      SQLGrammarException e = assertThrows(SQLGrammarException.class,
          () -> em.createNativeQuery("select * from Produces", Produce.class).getResultList());
      assertTrue(e.getMessage().startsWith("Could not prepare statement [Table \"PRODUCES\" not found"));
    });
  }

  @Test
  void lazyFarmIsNotLoadedByNativeQuery() {
    emf.runInTransaction(em -> {
      Produce apples = (Produce) em.createNativeQuery("select * from Produce where name = 'Apples'", Produce.class)
          .getSingleResult();
      assertFalse(org.hibernate.Hibernate.isInitialized(apples.getFarm()));
      assertEquals("Green Valley", apples.getFarm().getName());
      assertTrue(org.hibernate.Hibernate.isInitialized(apples.getFarm()));
    });
  }

  @Test
  void entitiesInsideNamedNativeQueryAreIgnoredByHibernate() {
    emf.runInTransaction(em -> {
      List<Object[]> rows = em.createNamedQuery("Produce.withFarmInline", Object[].class)
          .setParameter("region", "South")
          .getResultList();
      assertEquals(3, rows.size());
      assertEquals(8, rows.get(0).length);
      assertFalse(java.util.Arrays.stream(rows.get(0)).anyMatch(v -> v instanceof Produce));
    });
  }

  @Test
  void namedNativeQueryIsNotCheckedAtStartup() {
    try (EntityManagerFactory other = new org.hibernate.jpa.HibernatePersistenceConfiguration("native-check")
        .managedClasses(StartupCheckEntities.Basket.class)
        .jdbcUrl("jdbc:h2:mem:nativecheck")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(org.hibernate.tool.schema.Action.CREATE_DROP)
        .createEntityManagerFactory()) {
      other.runInTransaction(em -> {
        SQLGrammarException e = assertThrows(SQLGrammarException.class,
            () -> em.createNamedQuery("Basket.wrongTable").getResultList());
        assertTrue(e.getMessage().startsWith("Could not prepare statement [Table \"PRODUCES\" not found"));
      });
    }
  }

  @Test
  void namedJpqlQueryIsCheckedAtStartup() {
    RuntimeException e = assertThrows(RuntimeException.class,
        () -> new org.hibernate.jpa.HibernatePersistenceConfiguration("jpql-check")
            .managedClasses(StartupCheckEntities.Crate.class)
            .jdbcUrl("jdbc:h2:mem:jpqlcheck")
            .jdbcCredentials("sa", "")
            .createEntityManagerFactory());
    Throwable root = e;
    while (root.getCause() != null) {
      root = root.getCause();
    }
    assertEquals("org.hibernate.query.NamedQueryValidationException", root.getClass().getName());
    assertTrue(root.getMessage().contains(
        "Error in query named 'Produce.findAll': Could not resolve root entity 'Produces'"));
  }
}
