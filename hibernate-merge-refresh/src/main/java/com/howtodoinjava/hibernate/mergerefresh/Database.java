package com.howtodoinjava.hibernate.mergerefresh;

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
    EntityManagerFactory emf = new HibernatePersistenceConfiguration("merge-refresh")
        .managedClasses(Shipment.class)
        .jdbcUrl("jdbc:h2:mem:warehouse;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .property("hibernate.generate_statistics", true)
        .createEntityManagerFactory();
    emf.runInTransaction(em -> em.createNativeQuery(
            "create trigger shipment_received before insert on Shipment for each row call '"
                + ReceivedTrigger.class.getName() + "'")
        .executeUpdate());
    return emf;
  }

  public static Statistics statistics(EntityManagerFactory emf) {
    Statistics statistics = emf.unwrap(SessionFactory.class).getStatistics();
    statistics.clear();
    return statistics;
  }

  public static Shipment load(EntityManagerFactory emf, Long id) {
    return emf.callInTransaction(em -> em.find(Shipment.class, id));
  }
}
