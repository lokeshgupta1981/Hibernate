package com.howtodoinjava.hibernate.namedqueryerrors;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * The classic HibernateUtil helper from Hibernate 3 era tutorials, kept to show where the
 * "Initial SessionFactory creation failed." line comes from. It registers a broken named query.
 */
public final class HibernateUtil {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private static final SessionFactory sessionFactory = buildSessionFactory();

  private HibernateUtil() {
  }

  private static SessionFactory buildSessionFactory() {
    try {
      return new Configuration()
          .addAnnotatedClass(Volunteer.class)
          .addAnnotatedClass(Shift.class)
          .addAnnotatedClass(BrokenQueries.TableName.class)
          .setProperty("hibernate.connection.url", "jdbc:h2:mem:legacy")
          .buildSessionFactory();
    } catch (Throwable ex) {
      System.err.println("Initial SessionFactory creation failed." + ex);
      throw new ExceptionInInitializerError(ex);
    }
  }

  public static SessionFactory getSessionFactory() {
    return sessionFactory;
  }
}
