package com.howtodoinjava.hibernate.initialize;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  public static EntityManagerFactory create(boolean showSql) {
    return configuration(showSql).createEntityManagerFactory();
  }

  /** Same database, but Hibernate loads lazy collections in batches of the given size. */
  public static EntityManagerFactory createWithBatchFetch(boolean showSql, int batchSize) {
    return configuration(showSql)
        .property("hibernate.default_batch_fetch_size", batchSize)
        .createEntityManagerFactory();
  }

  /** Same database, with the hibernate.enable_lazy_load_no_trans setting turned on. */
  public static EntityManagerFactory createWithLazyLoadNoTrans(boolean showSql) {
    return configuration(showSql)
        .property("hibernate.enable_lazy_load_no_trans", true)
        .createEntityManagerFactory();
  }

  private static HibernatePersistenceConfiguration configuration(boolean showSql) {
    return new HibernatePersistenceConfiguration("initialize-proxy")
        .managedClasses(Team.class, Player.class)
        .jdbcUrl("jdbc:h2:mem:league;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .property("hibernate.generate_statistics", true)
        .showSql(showSql, false, false);
  }

  /** Saves three teams with their players and returns the team ids. */
  public static Long[] seed(EntityManagerFactory emf) {
    return emf.callInTransaction(em -> {
      Team lions = new Team("Lions");
      lions.addPlayer(new Player("Lokesh"));
      lions.addPlayer(new Player("Alex"));
      lions.addPlayer(new Player("Sara"));
      Team tigers = new Team("Tigers");
      tigers.addPlayer(new Player("Maria"));
      tigers.addPlayer(new Player("John"));
      Team eagles = new Team("Eagles");
      eagles.addPlayer(new Player("Emma"));
      for (Team team : new Team[] {lions, tigers, eagles}) {
        em.persist(team);
        team.getPlayers().forEach(em::persist);
      }
      return new Long[] {lions.getId(), tigers.getId(), eagles.getId()};
    });
  }

  public static Long playerId(EntityManagerFactory emf, String name) {
    return emf.callInTransaction(em -> em
        .createQuery("select p.id from Player p where p.name = :name", Long.class)
        .setParameter("name", name)
        .getSingleResult());
  }

  /** Clears the statistics so the next call counts only its own statements. */
  public static void resetCount(EntityManagerFactory emf) {
    emf.unwrap(SessionFactory.class).getStatistics().clear();
  }

  /** Number of JDBC statements prepared since the last reset. */
  public static long statements(EntityManagerFactory emf) {
    return emf.unwrap(SessionFactory.class).getStatistics().getPrepareStatementCount();
  }
}
