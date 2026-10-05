package com.howtodoinjava.hibernate.initialize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceUnitUtil;
import java.util.List;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.collection.spi.PersistentBag;
import org.hibernate.proxy.HibernateProxy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InitializeTest {

  private EntityManagerFactory emf;
  private Long[] teamIds;
  private Long lokeshId;

  @BeforeEach
  void setUp() {
    emf = Database.create(false);
    teamIds = Database.seed(emf);
    lokeshId = Database.playerId(emf, "Lokesh");
  }

  @AfterEach
  void tearDown() {
    emf.close();
  }

  private List<String> names(List<Player> players) {
    return players.stream().map(Player::getName).toList();
  }

  @Test
  void manyToOneLazyGivesProxyAndGetIdRunsNoSql() {
    emf.runInTransaction(em -> {
      Player lokesh = em.find(Player.class, lokeshId);
      Team team = lokesh.getTeam();
      assertTrue(team instanceof HibernateProxy);
      assertEquals("Team$HibernateProxy", team.getClass().getSimpleName());
      assertFalse(Hibernate.isInitialized(team));
      Database.resetCount(emf);
      assertEquals(teamIds[0], team.getId());
      assertEquals(0, Database.statements(emf));
      assertFalse(Hibernate.isInitialized(team));
    });
  }

  @Test
  void initializeProxyRunsOneSelectOnlyOnce() {
    emf.runInTransaction(em -> {
      Team team = em.find(Player.class, lokeshId).getTeam();
      Database.resetCount(emf);
      Hibernate.initialize(team);
      assertEquals(1, Database.statements(emf));
      assertTrue(Hibernate.isInitialized(team));
      assertEquals("Lions", team.getName());
      Hibernate.initialize(team);
      Hibernate.initialize(null);
      assertEquals(1, Database.statements(emf));
    });
  }

  @Test
  void initializedProxyKeepsProxyClassUntilUnproxy() {
    emf.runInTransaction(em -> {
      Team team = em.find(Player.class, lokeshId).getTeam();
      Hibernate.initialize(team);
      assertEquals("Team$HibernateProxy", team.getClass().getSimpleName());
      assertEquals(Team.class, Hibernate.getClass(team));
      Team real = Hibernate.unproxy(team, Team.class);
      assertEquals(Team.class, real.getClass());
      assertNotSame(team, real);
      assertSame(real, Hibernate.unproxy(team));
    });
  }

  @Test
  void unproxyInitializesAnUninitializedProxy() {
    emf.runInTransaction(em -> {
      Team team = em.getReference(Team.class, teamIds[1]);
      assertFalse(Hibernate.isInitialized(team));
      Database.resetCount(emf);
      Team real = Hibernate.unproxy(team, Team.class);
      assertEquals(1, Database.statements(emf));
      assertEquals("Tigers", real.getName());
      assertTrue(Hibernate.isInitialized(team));
    });
  }

  @Test
  void initializeProxyForMissingRowThrows() {
    emf.runInTransaction(em -> {
      Team team = em.getReference(Team.class, 999L);
      EntityNotFoundException e =
          assertThrows(EntityNotFoundException.class, () -> Hibernate.initialize(team));
      assertTrue(e.getMessage().startsWith("No row with the given identifier exists"));
    });
  }

  @Test
  void collectionIsPersistentBagAndInitializeLoadsIt() {
    emf.runInTransaction(em -> {
      Team lions = em.find(Team.class, teamIds[0]);
      List<Player> players = lions.getPlayers();
      assertTrue(players instanceof PersistentBag);
      assertFalse(Hibernate.isInitialized(players));
      Database.resetCount(emf);
      Hibernate.initialize(players);
      assertEquals(1, Database.statements(emf));
      assertTrue(Hibernate.isInitialized(players));
      assertEquals(List.of("Lokesh", "Alex", "Sara"), names(players));
    });
  }

  @Test
  void sizeContainsAndIsEmptyDoNotInitialize() {
    Long alexId = Database.playerId(emf, "Alex");
    emf.runInTransaction(em -> {
      Team lions = em.find(Team.class, teamIds[0]);
      Player alex = em.find(Player.class, alexId);
      Database.resetCount(emf);
      assertEquals(3, Hibernate.size(lions.getPlayers()));
      assertTrue(Hibernate.contains(lions.getPlayers(), alex));
      assertEquals(2, Database.statements(emf));
      assertFalse(Hibernate.isInitialized(lions.getPlayers()));
    });
    emf.runInTransaction(em -> {
      Team eagles = em.find(Team.class, teamIds[2]);
      Database.resetCount(emf);
      assertFalse(Hibernate.isEmpty(eagles.getPlayers()));
      assertEquals(1, Database.statements(emf));
      assertFalse(Hibernate.isInitialized(eagles.getPlayers()));
    });
  }

  @Test
  void persistenceUnitUtilIsLoadedAndLoad() {
    PersistenceUnitUtil util = emf.getPersistenceUnitUtil();
    emf.runInTransaction(em -> {
      Player lokesh = em.find(Player.class, lokeshId);
      assertFalse(util.isLoaded(lokesh, "team"));
      assertFalse(util.isLoaded(lokesh.getTeam()));
      Database.resetCount(emf);
      util.load(lokesh.getTeam());
      assertEquals(1, Database.statements(emf));
      assertTrue(util.isLoaded(lokesh.getTeam()));
      assertTrue(util.isLoaded(lokesh, "team"));
    });
    emf.runInTransaction(em -> {
      Team lions = em.find(Team.class, teamIds[0]);
      assertFalse(util.isLoaded(lions, "players"));
      Database.resetCount(emf);
      util.load(lions, "players");                      // Hibernate 7.4.11: no SQL for a lazy collection
      assertEquals(0, Database.statements(emf));
      assertFalse(util.isLoaded(lions, "players"));
      Hibernate.initialize(lions.getPlayers());
      assertTrue(util.isLoaded(lions, "players"));
    });
  }

  @Test
  void initializeProxyAfterCloseThrows() {
    Player detached = emf.callInTransaction(em -> em.find(Player.class, lokeshId));
    LazyInitializationException e = assertThrows(LazyInitializationException.class,
        () -> Hibernate.initialize(detached.getTeam()));
    assertEquals("Could not initialize proxy [com.howtodoinjava.hibernate.initialize.Team#"
        + teamIds[0] + "] - no session", e.getMessage());
    assertFalse(Hibernate.isInitialized(detached.getTeam()));
  }

  @Test
  void initializeCollectionAfterCloseThrows() {
    Team detached = emf.callInTransaction(em -> em.find(Team.class, teamIds[0]));
    LazyInitializationException e = assertThrows(LazyInitializationException.class,
        () -> Hibernate.initialize(detached.getPlayers()));
    assertEquals("Cannot lazily initialize collection of role "
        + "'com.howtodoinjava.hibernate.initialize.Team.players' with key '" + teamIds[0]
        + "' (no session)", e.getMessage());
  }

  @Test
  void initializeInsideTransactionThenUseAfterClose() {
    Team lions = emf.callInTransaction(em -> {
      Team team = em.find(Team.class, teamIds[0]);
      Hibernate.initialize(team.getPlayers());
      return team;
    });
    assertEquals(List.of("Lokesh", "Alex", "Sara"), names(lions.getPlayers()));
  }

  @Test
  void initializeProxyAndItsCollectionThenUseAfterClose() {
    Database.resetCount(emf);
    Team lions = emf.callInTransaction(em -> {
      Player lokesh = em.find(Player.class, lokeshId);
      Team team = lokesh.getTeam();
      Hibernate.initialize(team);
      Hibernate.initialize(team.getPlayers());
      return team;
    });
    assertEquals(3, Database.statements(emf));
    assertEquals("Lions", lions.getName());
    assertEquals(List.of("Lokesh", "Alex", "Sara"), names(lions.getPlayers()));
  }

  @Test
  void mergeThenInitializeWorksForDetachedEntity() {
    Team detached = emf.callInTransaction(em -> em.find(Team.class, teamIds[0]));
    List<String> names = emf.callInTransaction(em -> {
      Team managed = em.merge(detached);
      Hibernate.initialize(managed.getPlayers());
      return names(managed.getPlayers());
    });
    assertEquals(List.of("Lokesh", "Alex", "Sara"), names);
  }

  @Test
  void initializeInLoopRunsOnePlusNStatements() {
    Database.resetCount(emf);
    emf.runInTransaction(em -> {
      List<Team> teams = em.createQuery("from Team order by id", Team.class).getResultList();
      teams.forEach(t -> Hibernate.initialize(t.getPlayers()));
    });
    assertEquals(4, Database.statements(emf));
  }

  @Test
  void joinFetchRunsOneStatement() {
    Database.resetCount(emf);
    List<Team> teams = emf.callInTransaction(em -> em
        .createQuery("select t from Team t left join fetch t.players order by t.id", Team.class)
        .getResultList());
    assertEquals(1, Database.statements(emf));
    assertEquals(3, teams.size());
    assertEquals(List.of("Maria", "John"), names(teams.get(1).getPlayers()));
  }

  @Test
  void entityGraphRunsOneStatement() {
    Database.resetCount(emf);
    List<Team> teams = emf.callInTransaction(em -> {
      EntityGraph<Team> graph = em.createEntityGraph(Team.class);
      graph.addAttributeNode("players");
      return em.createQuery("from Team order by id", Team.class)
          .setHint("jakarta.persistence.fetchgraph", graph)
          .getResultList();
    });
    assertEquals(1, Database.statements(emf));
    assertEquals(3, teams.size());
    assertTrue(Hibernate.isInitialized(teams.get(2).getPlayers()));
  }

  @Test
  void batchFetchRunsTwoStatements() {
    emf.close();
    emf = Database.createWithBatchFetch(false, 10);
    Database.seed(emf);
    Database.resetCount(emf);
    emf.runInTransaction(em -> {
      List<Team> teams = em.createQuery("from Team order by id", Team.class).getResultList();
      Hibernate.initialize(teams.get(0).getPlayers());
      assertTrue(Hibernate.isInitialized(teams.get(2).getPlayers()));
      teams.forEach(t -> Hibernate.initialize(t.getPlayers()));
    });
    assertEquals(2, Database.statements(emf));
  }

  @Test
  void lazyLoadNoTransInitializesDetachedCollection() {
    emf.close();
    emf = Database.createWithLazyLoadNoTrans(false);
    Long[] ids = Database.seed(emf);
    Team detached = emf.callInTransaction(em -> em.find(Team.class, ids[0]));
    Database.resetCount(emf);
    Hibernate.initialize(detached.getPlayers());
    assertEquals(1, Database.statements(emf));
    assertEquals(List.of("Lokesh", "Alex", "Sara"), names(detached.getPlayers()));
  }

  @Test
  void callingSizeOnTheCollectionLoadsIt() {
    emf.runInTransaction(em -> {
      Team lions = em.find(Team.class, teamIds[0]);
      Database.resetCount(emf);
      assertEquals(3, lions.getPlayers().size());
      assertEquals(1, Database.statements(emf));
      assertTrue(Hibernate.isInitialized(lions.getPlayers()));
    });
  }

  @Test
  void loopingOverTheCollectionLoadsIt() {
    emf.runInTransaction(em -> {
      Team lions = em.find(Team.class, teamIds[0]);
      Database.resetCount(emf);
      int count = 0;
      for (Player ignored : lions.getPlayers()) {
        count++;
      }
      assertEquals(3, count);
      assertEquals(1, Database.statements(emf));
      assertTrue(Hibernate.isInitialized(lions.getPlayers()));
    });
  }

  @Test
  void initializedProxyAnswersGettersWithoutSql() {
    emf.runInTransaction(em -> {
      Team team = em.find(Player.class, lokeshId).getTeam();
      Hibernate.initialize(team);
      Database.resetCount(emf);
      assertEquals("Lions", team.getName());
      assertEquals(0, Database.statements(emf));
    });
  }

  private static void seedTeams(EntityManagerFactory emf, int count) {
    emf.runInTransaction(em -> {
      for (int i = 1; i <= count; i++) {
        Team team = new Team("Team " + i);
        team.addPlayer(new Player("Player " + i));
        em.persist(team);
        team.getPlayers().forEach(em::persist);
      }
    });
  }

  private static long loopCount(EntityManagerFactory emf) {
    Database.resetCount(emf);
    emf.runInTransaction(em -> em.createQuery("from Team", Team.class).getResultList()
        .forEach(t -> Hibernate.initialize(t.getPlayers())));
    return Database.statements(emf);
  }

  @Test
  void statementCountsWithHundredTeams() {
    emf.close();
    emf = Database.create(false);
    seedTeams(emf, 100);
    assertEquals(101, loopCount(emf));

    Database.resetCount(emf);
    emf.runInTransaction(em -> em
        .createQuery("select t from Team t left join fetch t.players", Team.class)
        .getResultList());
    assertEquals(1, Database.statements(emf));

    Database.resetCount(emf);
    emf.runInTransaction(em -> {
      EntityGraph<Team> graph = em.createEntityGraph(Team.class);
      graph.addAttributeNode("players");
      em.createQuery("from Team", Team.class)
          .setHint("jakarta.persistence.fetchgraph", graph)
          .getResultList();
    });
    assertEquals(1, Database.statements(emf));

    emf.close();
    emf = Database.createWithBatchFetch(false, 10);
    seedTeams(emf, 100);
    assertEquals(11, loopCount(emf));
  }
}
