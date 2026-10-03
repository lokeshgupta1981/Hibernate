package com.howtodoinjava.hibernate.ehcache;

import jakarta.persistence.EntityManagerFactory;

/** Started by ClasspathErrorsTest in a separate JVM: builds the factory and prints the error chain. */
public class StartCheck {

  public static void main(String[] args) {
    try (EntityManagerFactory emf = Database.create(false)) {
      System.out.println("started");
    } catch (Throwable e) {
      for (Throwable t = e; t != null; t = t.getCause()) {
        System.out.println(t.getClass().getName() + ": " + t.getMessage());
      }
    }
  }
}
