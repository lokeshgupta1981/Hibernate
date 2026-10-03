package com.howtodoinjava.hibernate.lazy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PersistenceUnitUtil;
import java.math.BigDecimal;
import java.util.List;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.collection.spi.PersistentBag;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LazyLoadingTest {

  private EntityManagerFactory emf;
  private PersistenceUnitUtil util;
  private Long greenBowlId;
  private Long pastaId;
  private Long mondayId;

  @BeforeEach
  void setUp() {
    emf = Database.create(false);
    util = emf.getPersistenceUnitUtil();
    greenBowlId = Database.seed(emf);
    pastaId = emf.callInTransaction(em -> em.createQuery(
        "select m.id from MenuItem m where m.name = 'Pasta'", Long.class).getSingleResult());
    mondayId = emf.callInTransaction(em -> em.createQuery(
        "select d.id from DailySpecial d where d.weekday = 'Monday'", Long.class).getSingleResult());
  }

  @AfterEach
  void tearDown() {
    emf.close();
  }

  private Statistics statistics() {
    return Database.statistics(emf);
  }

  // ---------- defaults ----------

  @Test
  void defaultFetchTypesOfTheAnnotations() throws Exception {
    assertEquals(FetchType.LAZY, OneToMany.class.getMethod("fetch").getDefaultValue());
    assertEquals(FetchType.LAZY, ManyToMany.class.getMethod("fetch").getDefaultValue());
    assertEquals(FetchType.LAZY, ElementCollection.class.getMethod("fetch").getDefaultValue());
    assertEquals(FetchType.EAGER, ManyToOne.class.getMethod("fetch").getDefaultValue());
    assertEquals(FetchType.EAGER, OneToOne.class.getMethod("fetch").getDefaultValue());
    assertEquals(FetchType.EAGER, Basic.class.getMethod("fetch").getDefaultValue());
  }

  // ---------- quick reference ----------

  @Test
  void quickReferenceRunsOneSelectPerStep() {
    emf.runInTransaction(em -> {
      Statistics stats = statistics();
      MenuItem pasta = em.find(MenuItem.class, pastaId);
      assertEquals(1, stats.getPrepareStatementCount());
      Restaurant restaurant = pasta.getRestaurant();
      assertEquals(1, stats.getPrepareStatementCount());
      assertEquals("Green Bowl", restaurant.getName());
      assertEquals(2, stats.getPrepareStatementCount());
      assertEquals(2, restaurant.getMenu().size());
      assertEquals(3, stats.getPrepareStatementCount());
    });
  }

  // ---------- lazy collection ----------

  @Test
  void findRestaurantLoadsOnlyTheRestaurantRow() {
    emf.runInTransaction(em -> {
      Statistics stats = statistics();
      Restaurant restaurant = em.find(Restaurant.class, greenBowlId);
      List<MenuItem> menu = restaurant.getMenu();
      assertEquals(1, stats.getPrepareStatementCount());
      assertTrue(menu instanceof PersistentBag<?>);
      assertEquals("org.hibernate.collection.spi.PersistentBag", menu.getClass().getName());
      assertFalse(Hibernate.isInitialized(menu));
      assertFalse(util.isLoaded(restaurant, "menu"));

      assertEquals(2, menu.size());
      assertEquals(2, stats.getPrepareStatementCount());
      assertTrue(Hibernate.isInitialized(menu));
      assertTrue(util.isLoaded(restaurant, "menu"));
    });
  }

  @Test
  void iteratingCheckingEmptyOrPrintingTheCollectionLoadsIt() {
    emf.runInTransaction(em -> {
      Restaurant r = em.find(Restaurant.class, greenBowlId);
      assertFalse(r.getMenu().isEmpty());
      assertTrue(Hibernate.isInitialized(r.getMenu()));
    });
    emf.runInTransaction(em -> {
      Restaurant r = em.find(Restaurant.class, greenBowlId);
      r.getMenu().iterator();
      assertTrue(Hibernate.isInitialized(r.getMenu()));
    });
    emf.runInTransaction(em -> {
      Restaurant r = em.find(Restaurant.class, greenBowlId);
      assertEquals("[Pasta, Salad]", r.getMenu().toString());
      assertTrue(Hibernate.isInitialized(r.getMenu()));
    });
  }

  @Test
  void elementsOfALazyCollectionAreRealEntities() {
    emf.runInTransaction(em -> {
      Restaurant r = em.find(Restaurant.class, greenBowlId);
      assertSame(MenuItem.class, r.getMenu().get(0).getClass());
    });
  }

  // ---------- lazy to-one proxy ----------

  @Test
  void lazyManyToOneIsAProxyUntilAFieldIsRead() {
    emf.runInTransaction(em -> {
      Statistics stats = statistics();
      MenuItem pasta = em.find(MenuItem.class, pastaId);
      Restaurant restaurant = pasta.getRestaurant();
      assertEquals(1, stats.getPrepareStatementCount());
      assertTrue(restaurant.getClass().getName().endsWith("Restaurant$HibernateProxy"));
      assertTrue(restaurant instanceof Restaurant);
      assertFalse(Hibernate.isInitialized(restaurant));

      assertEquals(greenBowlId, restaurant.getId());
      assertEquals(1, stats.getPrepareStatementCount());            // getId(): no SQL

      assertEquals("Green Bowl", restaurant.getName());
      assertEquals(2, stats.getPrepareStatementCount());            // getName(): one SELECT
      assertTrue(Hibernate.isInitialized(restaurant));
    });
  }

  @Test
  void hibernateGetClassInitializesTheProxy() {
    emf.runInTransaction(em -> {
      Restaurant restaurant = em.find(MenuItem.class, pastaId).getRestaurant();
      Statistics stats = statistics();
      assertSame(Restaurant.class, Hibernate.getClass(restaurant));
      assertEquals(1, stats.getPrepareStatementCount());
      assertTrue(Hibernate.isInitialized(restaurant));
    });
  }

  // ---------- eager ----------

  @Test
  void eagerManyToOneIsJoinedByFind() {
    emf.runInTransaction(em -> {
      Statistics stats = statistics();
      DailySpecial monday = em.find(DailySpecial.class, mondayId);
      assertEquals(1, stats.getPrepareStatementCount());
      assertSame(MenuItem.class, monday.getItem().getClass());
      assertEquals("Pasta", monday.getItem().getName());
      assertEquals(1, stats.getPrepareStatementCount());
    });
  }

  @Test
  void eagerManyToOneInAQueryRunsOneSelectPerRow() {
    Statistics stats = statistics();
    int specials = emf.callInTransaction(em ->
        em.createQuery("select d from DailySpecial d", DailySpecial.class).getResultList().size());
    assertEquals(3, specials);
    assertEquals(4, stats.getPrepareStatementCount());               // 1 + 3
  }

  // ---------- LazyInitializationException ----------

  @Test
  void lazyCollectionAfterTheTransactionThrows() {
    Restaurant detached = emf.callInTransaction(em -> em.find(Restaurant.class, greenBowlId));
    List<MenuItem> menu = detached.getMenu();                         // no exception yet
    LazyInitializationException e = assertThrows(LazyInitializationException.class, menu::size);
    assertEquals("Cannot lazily initialize collection of role "
        + "'com.howtodoinjava.hibernate.lazy.Restaurant.menu' with key '" + greenBowlId + "' (no session)",
        e.getMessage());
  }

  @Test
  void proxyAfterTheTransactionThrowsButGetIdWorks() {
    MenuItem detached = emf.callInTransaction(em -> em.find(MenuItem.class, pastaId));
    assertEquals(greenBowlId, detached.getRestaurant().getId());
    LazyInitializationException e = assertThrows(LazyInitializationException.class,
        () -> detached.getRestaurant().getName());
    assertEquals("Could not initialize proxy [com.howtodoinjava.hibernate.lazy.Restaurant#"
        + greenBowlId + "] - no session", e.getMessage());
  }

  // ---------- fixes ----------

  @Test
  void joinFetchLoadsTheMenuInOneSelect() {
    Statistics stats = statistics();
    Restaurant r = emf.callInTransaction(em -> em.createQuery(
        "select r from Restaurant r join fetch r.menu where r.id = :id", Restaurant.class)
        .setParameter("id", greenBowlId).getSingleResult());
    assertEquals(1, stats.getPrepareStatementCount());
    assertEquals("[Pasta, Salad]", r.getMenu().toString());          // after commit
  }

  @Test
  void joinFetchIsAnInnerJoinAndLeftJoinFetchKeepsEmptyRestaurants() {
    Long emptyId = emf.callInTransaction(em -> {
      Restaurant soupBar = new Restaurant("Soup Bar");
      em.persist(soupBar);
      return soupBar.getId();
    });
    long inner = emf.callInTransaction(em -> (long) em.createQuery(
        "select r from Restaurant r join fetch r.menu where r.id = :id", Restaurant.class)
        .setParameter("id", emptyId).getResultList().size());
    assertEquals(0, inner);
    List<Restaurant> left = emf.callInTransaction(em -> em.createQuery(
        "select r from Restaurant r left join fetch r.menu where r.id = :id", Restaurant.class)
        .setParameter("id", emptyId).getResultList());
    assertEquals(1, left.size());
    assertTrue(left.get(0).getMenu().isEmpty());                    // loaded, usable after commit
  }

  @Test
  void fetchGraphHintSkipsAnEagerAssociation() {
    Statistics stats = statistics();
    List<DailySpecial> specials = emf.callInTransaction(em -> em.createQuery(
        "select d from DailySpecial d", DailySpecial.class)
        .setHint("jakarta.persistence.fetchgraph", em.createEntityGraph(DailySpecial.class))
        .getResultList());
    assertEquals(3, specials.size());
    assertEquals(1, stats.getPrepareStatementCount());
    assertFalse(Hibernate.isInitialized(specials.get(0).getItem()));
  }

  @Test
  void entityGraphWithFindLoadsTheMenuInOneSelect() {
    Statistics stats = statistics();
    Restaurant r = emf.callInTransaction(em -> {
      EntityGraph<Restaurant> graph = em.createEntityGraph(Restaurant.class);
      graph.addAttributeNode("menu");
      return em.find(graph, greenBowlId);
    });
    assertEquals(1, stats.getPrepareStatementCount());
    assertEquals("[Pasta, Salad]", r.getMenu().toString());
  }

  @Test
  void namedEntityGraphAsQueryHintLoadsTheMenuInOneSelect() {
    Statistics stats = statistics();
    Restaurant r = emf.callInTransaction(em -> em.createQuery(
        "select r from Restaurant r where r.id = :id", Restaurant.class)
        .setParameter("id", greenBowlId)
        .setHint("jakarta.persistence.fetchgraph", em.getEntityGraph("Restaurant.menu"))
        .getSingleResult());
    assertEquals(1, stats.getPrepareStatementCount());
    assertEquals("[Pasta, Salad]", r.getMenu().toString());
  }

  @Test
  void hibernateInitializeRunsTheSecondSelectInsideTheTransaction() {
    Statistics stats = statistics();
    Restaurant r = emf.callInTransaction(em -> {
      Restaurant restaurant = em.find(Restaurant.class, greenBowlId);
      Hibernate.initialize(restaurant.getMenu());
      return restaurant;
    });
    assertEquals(2, stats.getPrepareStatementCount());
    assertEquals("[Pasta, Salad]", r.getMenu().toString());
  }

  @Test
  void hibernateInitializeOnADetachedEntityThrows() {
    Restaurant detached = emf.callInTransaction(em -> em.find(Restaurant.class, greenBowlId));
    assertThrows(LazyInitializationException.class, () -> Hibernate.initialize(detached.getMenu()));
  }

  @Test
  void persistenceUnitUtilLoadDoesNotLoadACollectionOfAnUnenhancedEntity() {
    Statistics stats = statistics();
    Restaurant r = emf.callInTransaction(em -> {
      Restaurant restaurant = em.find(Restaurant.class, greenBowlId);
      emf.getPersistenceUnitUtil().load(restaurant, "menu");        // JPA 3.2
      assertFalse(Hibernate.isInitialized(restaurant.getMenu()));
      return restaurant;
    });
    assertEquals(1, stats.getPrepareStatementCount());
    assertThrows(LazyInitializationException.class, () -> r.getMenu().size());
  }

  @Test
  void dtoProjectionReadsOnlyTheColumnsItNeeds() {
    Statistics stats = statistics();
    List<MenuLine> lines = emf.callInTransaction(em -> em.createQuery(
        "select r.name, m.name, m.price from MenuItem m join m.restaurant r "
            + "where r.id = :id order by m.name", MenuLine.class)
        .setParameter("id", greenBowlId).getResultList());
    assertEquals(1, stats.getPrepareStatementCount());
    assertEquals(List.of(
        new MenuLine("Green Bowl", "Pasta", new BigDecimal("8.00")),
        new MenuLine("Green Bowl", "Salad", new BigDecimal("6.00"))), lines);
  }

  // ---------- N+1 ----------

  @Test
  void readingEachMenuInALoopRunsNPlusOneSelects() {
    Statistics stats = statistics();
    int items = emf.callInTransaction(em -> em.createQuery(
        "select r from Restaurant r order by r.id", Restaurant.class)
        .getResultList().stream().mapToInt(r -> r.getMenu().size()).sum());
    assertEquals(6, items);
    assertEquals(4, stats.getPrepareStatementCount());               // 1 + 3 restaurants
  }

  @Test
  void joinFetchTurnsNPlusOneIntoOneSelect() {
    Statistics stats = statistics();
    List<Restaurant> all = emf.callInTransaction(em -> em.createQuery(
        "select r from Restaurant r join fetch r.menu order by r.id", Restaurant.class).getResultList());
    assertEquals(3, all.size());                                     // no duplicates, even without distinct
    assertEquals(6, all.stream().mapToInt(r -> r.getMenu().size()).sum());
    assertEquals(1, stats.getPrepareStatementCount());
  }

  @Test
  void batchFetchSizeLoadsAllMenusInOneExtraSelect() {
    try (EntityManagerFactory batch = Database.configuration(false)
        .jdbcUrl("jdbc:h2:mem:batchtest;DB_CLOSE_DELAY=-1")
        .property("hibernate.default_batch_fetch_size", "10")
        .createEntityManagerFactory()) {
      Database.seed(batch);
      Statistics stats = Database.statistics(batch);
      int items = batch.callInTransaction(em -> em.createQuery(
          "select r from Restaurant r order by r.id", Restaurant.class)
          .getResultList().stream().mapToInt(r -> r.getMenu().size()).sum());
      assertEquals(6, items);
      assertEquals(2, stats.getPrepareStatementCount());
    }
  }

  @Test
  void enableLazyLoadNoTransOpensATemporarySessionPerAccess() {
    try (EntityManagerFactory noTrans = Database.configuration(false)
        .jdbcUrl("jdbc:h2:mem:notranstest;DB_CLOSE_DELAY=-1")
        .property("hibernate.enable_lazy_load_no_trans", "true")
        .createEntityManagerFactory()) {
      Long id = Database.seed(noTrans);
      Restaurant detached = noTrans.callInTransaction(em -> em.find(Restaurant.class, id));
      Statistics stats = Database.statistics(noTrans);
      assertEquals(2, detached.getMenu().size());                    // no exception
      assertEquals(1, stats.getPrepareStatementCount());
      assertEquals(1, stats.getSessionOpenCount());                  // a new session just for this
    }
  }

  // ---------- @Basic(fetch = LAZY) ----------

  @Test
  void basicLazyIsIgnoredWithoutBytecodeEnhancement() {
    assertFalse(PersistentAttributeInterceptable.class.isAssignableFrom(MenuItem.class));
    MenuItem detached = emf.callInTransaction(em -> em.find(MenuItem.class, pastaId));
    assertTrue(util.isLoaded(detached, "description"));
    assertEquals("Pasta of the day", detached.getDescription());     // no exception after commit
  }

  @Test
  void basicLazyWorksWithBytecodeEnhancement() {
    assertTrue(PersistentAttributeInterceptable.class.isAssignableFrom(MenuCard.class));
    emf.runInTransaction(em -> {
      Statistics stats = statistics();
      MenuCard card = em.createQuery("select c from MenuCard c", MenuCard.class).getSingleResult();
      assertEquals(1, stats.getPrepareStatementCount());
      assertFalse(util.isLoaded(card, "text"));
      assertEquals("Pasta 8.00, Salad 6.00 ...", card.getText());
      assertEquals(2, stats.getPrepareStatementCount());
      assertTrue(util.isLoaded(card, "text"));
    });
  }

  @Test
  void enhancedLazyBasicAfterTheTransactionThrows() {
    MenuCard detached = emf.callInTransaction(em ->
        em.createQuery("select c from MenuCard c", MenuCard.class).getSingleResult());
    assertThrows(LazyInitializationException.class, detached::getText);
  }
}
