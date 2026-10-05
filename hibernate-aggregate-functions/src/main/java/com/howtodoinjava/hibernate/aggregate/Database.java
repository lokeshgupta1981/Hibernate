package com.howtodoinjava.hibernate.aggregate;

import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  public static EntityManagerFactory create(boolean showSql) {
    return new HibernatePersistenceConfiguration("aggregate-functions")
        .managedClasses(Runner.class, Club.class)
        .jdbcUrl("jdbc:h2:mem:marathon;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .createEntityManagerFactory();
  }

  /** Six runners of a city marathon. John did not finish, Maria has no club. */
  public static void loadResults(EntityManagerFactory emf) {
    emf.runInTransaction(em -> {
      Club striders = new Club("Delhi Striders");
      Club thames = new Club("Thames Runners");
      Club owls = new Club("Night Owls");     // no members
      em.persist(striders);
      em.persist(thames);
      em.persist(owls);

      em.persist(new Runner("Lokesh", "30-39", "Delhi", 215, new BigDecimal("40.00"), striders));
      em.persist(new Runner("Priya", "20-29", "Delhi", 182, new BigDecimal("40.00"), striders));
      em.persist(new Runner("Alex", "30-39", "London", 198, new BigDecimal("50.00"), thames));
      em.persist(new Runner("Emma", "20-29", "London", 205, new BigDecimal("50.00"), thames));
      em.persist(new Runner("John", "40-49", "London", null, new BigDecimal("50.00"), thames));
      em.persist(new Runner("Maria", "40-49", "Madrid", 240, new BigDecimal("40.00"), null));
    });
  }
}
