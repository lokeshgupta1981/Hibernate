package com.howtodoinjava.hibernate.initialize;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitUtil;
import java.util.List;
import org.hibernate.Hibernate;

public class InitializeDemo {

  public static void main(String[] args) {
    Long lionsId;
    Long lokeshId;

    try (EntityManagerFactory emf = Database.create(true)) {
      step("0. Save three teams and six players");
      Long[] teamIds = Database.seed(emf);
      lionsId = teamIds[0];
      lokeshId = Database.playerId(emf, "Lokesh");

      step("1. Load a player: the team is a proxy");
      emf.runInTransaction(em -> {
        Player lokesh = em.find(Player.class, lokeshId);
        Team team = lokesh.getTeam();
        print("team.getClass()", team.getClass().getSimpleName());
        print("Hibernate.isInitialized(team)", Hibernate.isInitialized(team));
        print("team.getId()", team.getId());
        print("Hibernate.isInitialized(team)", Hibernate.isInitialized(team));

        System.out.println("-- Hibernate.initialize(team)");
        Hibernate.initialize(team);
        print("Hibernate.isInitialized(team)", Hibernate.isInitialized(team));
        print("team.getName()", team.getName());
        print("team.getClass()", team.getClass().getSimpleName());
        print("Hibernate.getClass(team)", Hibernate.getClass(team).getSimpleName());

        Team real = Hibernate.unproxy(team, Team.class);
        print("Hibernate.unproxy(team).getClass()", real.getClass().getSimpleName());
        print("real == team", real == team);

        System.out.println("-- Hibernate.initialize(team) again");
        Hibernate.initialize(team);
      });

      step("2. Load a team: the players collection is not initialized");
      emf.runInTransaction(em -> {
        Team lions = em.find(Team.class, lionsId);
        List<Player> players = lions.getPlayers();
        print("players.getClass()", players.getClass().getSimpleName());
        print("Hibernate.isInitialized(players)", Hibernate.isInitialized(players));

        System.out.println("-- Hibernate.initialize(players)");
        Hibernate.initialize(players);
        print("Hibernate.isInitialized(players)", Hibernate.isInitialized(players));
        print("players", players);
      });

      step("3. Check without loading: Hibernate.size(), isEmpty() and contains()");
      Long alexId = Database.playerId(emf, "Alex");
      emf.runInTransaction(em -> {
        Team lions = em.find(Team.class, lionsId);
        Player alex = em.find(Player.class, alexId);
        System.out.println("-- Hibernate.size(players)");
        print("Hibernate.size(players)", Hibernate.size(lions.getPlayers()));
        System.out.println("-- Hibernate.contains(players, alex)");
        print("Hibernate.contains(players, alex)", Hibernate.contains(lions.getPlayers(), alex));
        print("Hibernate.isInitialized(players)", Hibernate.isInitialized(lions.getPlayers()));
      });
      emf.runInTransaction(em -> {
        Team eagles = em.find(Team.class, teamIds[2]);
        System.out.println("-- Hibernate.isEmpty(players)");
        print("Hibernate.isEmpty(players)", Hibernate.isEmpty(eagles.getPlayers()));
        print("Hibernate.isInitialized(players)", Hibernate.isInitialized(eagles.getPlayers()));
      });

      step("4. JPA: PersistenceUnitUtil.isLoaded() and load()");
      PersistenceUnitUtil util = emf.getPersistenceUnitUtil();
      emf.runInTransaction(em -> {
        Player lokesh = em.find(Player.class, lokeshId);
        print("util.isLoaded(lokesh, \"team\")", util.isLoaded(lokesh, "team"));
        print("util.isLoaded(lokesh.getTeam())", util.isLoaded(lokesh.getTeam()));
        System.out.println("-- util.load(lokesh.getTeam())");
        util.load(lokesh.getTeam());
        print("util.isLoaded(lokesh.getTeam())", util.isLoaded(lokesh.getTeam()));
        print("util.isLoaded(lokesh, \"team\")", util.isLoaded(lokesh, "team"));
      });
      emf.runInTransaction(em -> {
        Team lions = em.find(Team.class, lionsId);
        print("util.isLoaded(lions, \"players\")", util.isLoaded(lions, "players"));
        System.out.println("-- util.load(lions, \"players\")");
        util.load(lions, "players");
        print("util.isLoaded(lions, \"players\")", util.isLoaded(lions, "players"));
        System.out.println("-- Hibernate.initialize(lions.getPlayers())");
        Hibernate.initialize(lions.getPlayers());
        print("util.isLoaded(lions, \"players\")", util.isLoaded(lions, "players"));
      });

      step("5. Initialize after the EntityManager is closed");
      Player detached = emf.callInTransaction(em -> em.find(Player.class, lokeshId));
      try {
        Hibernate.initialize(detached.getTeam());
      } catch (RuntimeException e) {
        print("Hibernate.initialize(team)", e.getClass().getName() + ": " + e.getMessage());
      }
      Team detachedTeam = emf.callInTransaction(em -> em.find(Team.class, lionsId));
      try {
        Hibernate.initialize(detachedTeam.getPlayers());
      } catch (RuntimeException e) {
        print("Hibernate.initialize(players)", e.getClass().getName() + ": " + e.getMessage());
      }

      step("6. Initialize inside the transaction, use after it");
      Team loaded = emf.callInTransaction(em -> {
        Player lokesh = em.find(Player.class, lokeshId);
        Team team = lokesh.getTeam();
        Hibernate.initialize(team);
        Hibernate.initialize(team.getPlayers());
        return team;
      });
      print("team after close", loaded.getName());
      print("players after close", loaded.getPlayers());

      step("7. N+1: Hibernate.initialize() in a loop");
      Database.resetCount(emf);
      emf.runInTransaction(em -> {
        List<Team> teams = em.createQuery("from Team order by id", Team.class).getResultList();
        teams.forEach(t -> Hibernate.initialize(t.getPlayers()));
      });
      print("statements", Database.statements(emf));

      step("8. join fetch");
      Database.resetCount(emf);
      emf.runInTransaction(em -> {
        List<Team> teams = em.createQuery(
                "select t from Team t left join fetch t.players order by t.id", Team.class)
            .getResultList();
        print("teams", teams.size());
      });
      print("statements", Database.statements(emf));

      step("9. Entity graph");
      Database.resetCount(emf);
      emf.runInTransaction(em -> {
        EntityGraph<Team> graph = em.createEntityGraph(Team.class);
        graph.addAttributeNode("players");
        List<Team> teams = em.createQuery("from Team order by id", Team.class)
            .setHint("jakarta.persistence.fetchgraph", graph)
            .getResultList();
        print("teams", teams.size());
      });
      print("statements", Database.statements(emf));
    }

    step("10. Batch fetching: hibernate.default_batch_fetch_size = 10");
    try (EntityManagerFactory emf = Database.createWithBatchFetch(true, 10)) {
      Database.seed(emf);
      Database.resetCount(emf);
      emf.runInTransaction(em -> {
        List<Team> teams = em.createQuery("from Team order by id", Team.class).getResultList();
        teams.forEach(t -> Hibernate.initialize(t.getPlayers()));
      });
      print("statements", Database.statements(emf));
    }

    step("11. hibernate.enable_lazy_load_no_trans = true");
    try (EntityManagerFactory emf = Database.createWithLazyLoadNoTrans(true)) {
      Long[] ids = Database.seed(emf);
      Team detached = emf.callInTransaction(em -> em.find(Team.class, ids[0]));
      Database.resetCount(emf);
      Hibernate.initialize(detached.getPlayers());
      print("players", detached.getPlayers());
      print("statements", Database.statements(emf));
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("=== " + title + " ===");
  }

  private static void print(String label, Object value) {
    System.out.println("   " + label + " = " + value);
  }
}
