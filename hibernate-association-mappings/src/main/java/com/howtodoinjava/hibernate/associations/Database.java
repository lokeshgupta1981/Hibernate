package com.howtodoinjava.hibernate.associations;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  public static EntityManagerFactory create(boolean showSql) {
    return new HibernatePersistenceConfiguration("association-mappings")
        .managedClasses(Member.class, MembershipCard.class, Workout.class,
            FitnessClass.class, Trainer.class)
        .jdbcUrl("jdbc:h2:mem:gym;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .property("hibernate.generate_statistics", true)
        .createEntityManagerFactory();
  }

  public static long count(EntityManagerFactory emf, String entity) {
    return emf.callInTransaction(em ->
        em.createQuery("select count(*) from " + entity, Long.class).getSingleResult());
  }

  public static Object scalar(EntityManagerFactory emf, String sql) {
    return emf.callInTransaction(em -> em.createNativeQuery(sql).getSingleResult());
  }
}
