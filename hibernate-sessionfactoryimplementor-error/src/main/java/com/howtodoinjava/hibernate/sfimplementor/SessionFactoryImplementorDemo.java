package com.howtodoinjava.hibernate.sfimplementor;

import jakarta.persistence.EntityManagerFactory;
import java.lang.reflect.InvocationTargetException;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;

public class SessionFactoryImplementorDemo {

  private static final String OLD_NAME = "org/hibernate/engine/SessionFactoryImplementor";

  public static void main(String[] args) throws Exception {
    try (EntityManagerFactory emf = Database.create(true)) {
      System.out.println("== 1. Hibernate 7.4: where is SessionFactoryImplementor?");
      for (String name : new String[] {
          "org.hibernate.engine.SessionFactoryImplementor",
          "org.hibernate.engine.spi.SessionFactoryImplementor"}) {
        try {
          Class.forName(name);
          System.out.println("found:     " + name);
        } catch (ClassNotFoundException e) {
          System.out.println("not found: " + name);
        }
      }

      System.out.println("== 2. Spring 3.2 orm.hibernate3.HibernateTransactionManager on Hibernate 7.4");
      // loaded by name: spring-orm 3.2 is a test-scope dependency of this project
      Class<?> txManagerClass = Class.forName("org.springframework.orm.hibernate3.HibernateTransactionManager");
      Object txManager = txManagerClass.getDeclaredConstructor().newInstance();
      txManagerClass.getMethod("setSessionFactory", SessionFactory.class)
          .invoke(txManager, emf.unwrap(SessionFactory.class));
      try {
        txManagerClass.getMethod("afterPropertiesSet").invoke(txManager);
      } catch (InvocationTargetException e) {
        printTrace(e.getCause());
      }

      System.out.println("== 3. Which jar contains the failing class?");
      Class<?> utils = Class.forName("org.springframework.orm.hibernate3.SessionFactoryUtils");
      System.out.println(utils.getProtectionDomain().getCodeSource().getLocation());

      System.out.println("== 4. Classes in jars that still use " + OLD_NAME);
      ClasspathScanner.findReferences(OLD_NAME).forEach(System.out::println);

      System.out.println("== 5. Hibernate 7: unwrap the SPI interface");
      Database.saveSampleData(emf);
      SessionFactoryImplementor sfi = emf.unwrap(SessionFactoryImplementor.class);
      System.out.println("same object as SessionFactory: " + (sfi == emf.unwrap(SessionFactory.class)));
      System.out.println("dialect: " + sfi.getJdbcServices().getDialect().getClass().getSimpleName());
      System.out.println("entity: " + sfi.getMappingMetamodel().getEntityDescriptor(Fixture.class).getEntityName());
      emf.runInTransaction(em -> em.createQuery("select f from Fixture f order by f.kickoff", Fixture.class)
          .getResultList().forEach(System.out::println));
    }
  }

  private static void printTrace(Throwable e) {
    System.out.println(e);
    StackTraceElement[] frames = e.getStackTrace();
    for (int i = 0; i < Math.min(3, frames.length); i++) {
      System.out.println("\tat " + frames[i]);
    }
    System.out.println("\t...");
    if (e.getCause() != null) {
      System.out.println("Caused by: " + e.getCause());
    }
  }
}
