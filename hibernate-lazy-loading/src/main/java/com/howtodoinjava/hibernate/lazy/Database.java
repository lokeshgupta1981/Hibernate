package com.howtodoinjava.hibernate.lazy;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.stat.Statistics;
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

  public static HibernatePersistenceConfiguration configuration(boolean showSql) {
    return new HibernatePersistenceConfiguration("lazy-loading")
        .managedClasses(Restaurant.class, MenuItem.class, DailySpecial.class, MenuCard.class)
        .jdbcUrl("jdbc:h2:mem:restaurants;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .property("hibernate.generate_statistics", "true");
  }

  /** Resets the statement counter, so a step can count its own SQL. */
  public static Statistics statistics(EntityManagerFactory emf) {
    Statistics statistics = emf.unwrap(SessionFactory.class).getStatistics();
    statistics.clear();
    return statistics;
  }

  /** Three restaurants with two menu items each, three daily specials and one menu card. Returns the id of "Green Bowl". */
  public static Long seed(EntityManagerFactory emf) {
    return emf.callInTransaction(em -> {
      Restaurant greenBowl = new Restaurant("Green Bowl");
      greenBowl.addItem(new MenuItem("Pasta", "8.00"));
      greenBowl.addItem(new MenuItem("Salad", "6.00"));
      Restaurant tacoTown = new Restaurant("Taco Town");
      tacoTown.addItem(new MenuItem("Tacos", "5.00"));
      tacoTown.addItem(new MenuItem("Nachos", "4.00"));
      Restaurant spiceHut = new Restaurant("Spice Hut");
      spiceHut.addItem(new MenuItem("Curry", "9.00"));
      spiceHut.addItem(new MenuItem("Naan", "2.00"));
      em.persist(greenBowl);
      em.persist(tacoTown);
      em.persist(spiceHut);

      em.persist(new DailySpecial("Monday", greenBowl.getMenu().get(0)));
      em.persist(new DailySpecial("Tuesday", tacoTown.getMenu().get(0)));
      em.persist(new DailySpecial("Wednesday", spiceHut.getMenu().get(0)));

      em.persist(new MenuCard("Green Bowl dinner menu", "Pasta 8.00, Salad 6.00 ..."));
      return greenBowl.getId();
    });
  }
}
