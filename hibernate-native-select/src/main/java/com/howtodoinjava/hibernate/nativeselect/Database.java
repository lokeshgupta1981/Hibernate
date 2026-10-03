package com.howtodoinjava.hibernate.nativeselect;

import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  public static EntityManagerFactory create(boolean showSql) {
    return new HibernatePersistenceConfiguration("native-select")
        .managedClasses(Farm.class, Produce.class)
        .jdbcUrl("jdbc:h2:mem:farmshop;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .createEntityManagerFactory();
  }

  /** Two farms and six kinds of produce. */
  public static void insertSampleData(EntityManagerFactory emf) {
    emf.runInTransaction(em -> {
      Farm greenValley = new Farm("Green Valley", "North");
      Farm sunnyAcres = new Farm("Sunny Acres", "South");
      em.persist(greenValley);
      em.persist(sunnyAcres);

      LocalDate day = LocalDate.of(2026, 9, 28);
      em.persist(new Produce("Apples", "fruit", "2.50", day, greenValley));
      em.persist(new Produce("Pears", "fruit", "3.00", day, greenValley));
      em.persist(new Produce("Carrots", "vegetable", "1.20", day.plusDays(1), greenValley));
      em.persist(new Produce("Tomatoes", "vegetable", "2.80", day.plusDays(1), sunnyAcres));
      em.persist(new Produce("Strawberries", "fruit", "6.00", day.plusDays(2), sunnyAcres));
      em.persist(new Produce("Potatoes", "vegetable", "0.90", day.plusDays(2), sunnyAcres));
    });
  }
}
