package com.howtodoinjava.hibernate.delete;

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
    return new HibernatePersistenceConfiguration("delete-entities")
        .managedClasses(Newsletter.class, Subscriber.class)
        .jdbcUrl("jdbc:h2:mem:newsletters;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .property("hibernate.generate_statistics", true)
        .createEntityManagerFactory();
  }

  /** Creates the "Java Weekly" newsletter with Lokesh (confirmed), Alex and Brian. */
  public static Long seed(EntityManagerFactory emf) {
    return emf.callInTransaction(em -> {
      Newsletter javaWeekly = new Newsletter("Java Weekly");
      javaWeekly.addSubscriber(new Subscriber("Lokesh", true));
      javaWeekly.addSubscriber(new Subscriber("Alex", false));
      javaWeekly.addSubscriber(new Subscriber("Brian", false));
      em.persist(javaWeekly);
      javaWeekly.getSubscribers().forEach(em::persist);
      return javaWeekly.getId();
    });
  }

  public static Long subscriberId(EntityManagerFactory emf, String name) {
    return emf.callInTransaction(em -> em
        .createQuery("select s.id from Subscriber s where s.name = :name", Long.class)
        .setParameter("name", name)
        .getSingleResult());
  }

  public static long count(EntityManagerFactory emf, String entity) {
    return emf.callInTransaction(em ->
        em.createQuery("select count(*) from " + entity, Long.class).getSingleResult());
  }

  public static long statements(EntityManagerFactory emf) {
    return emf.unwrap(SessionFactory.class).getStatistics().getPrepareStatementCount();
  }

  public static void resetStatistics(EntityManagerFactory emf) {
    emf.unwrap(SessionFactory.class).getStatistics().clear();
  }
}
