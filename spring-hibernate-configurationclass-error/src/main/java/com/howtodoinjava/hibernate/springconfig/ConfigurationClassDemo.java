package com.howtodoinjava.hibernate.springconfig;

import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;

public class ConfigurationClassDemo {

  public static void main(String[] args) {
    System.setProperty("org.jboss.logging.provider", "slf4j");

    System.out.println("== 1. XML with configurationClass on Spring 7's LocalSessionFactoryBean");
    try (var context = new GenericXmlApplicationContext("legacy/session-factory-broken.xml")) {
      System.out.println("started (unexpected)");
    } catch (BeanCreationException e) {
      System.out.println(e.getClass().getName() + ": " + e.getMessage());
      System.out.println("Root cause: " + e.getMostSpecificCause().getClass().getName());
    }

    System.out.println();
    System.out.println("== 2. Same XML without configurationClass (packagesToScan)");
    try (var context = new GenericXmlApplicationContext("legacy/session-factory-packages.xml")) {
      SessionFactory sessionFactory = context.getBean(SessionFactory.class);
      sessionFactory.inTransaction(session -> {
        session.persist(new Showtime("Dune", 1, LocalDateTime.of(2026, 10, 9, 18, 30)));
        session.persist(new Showtime("Dune", 2, LocalDateTime.of(2026, 10, 9, 21, 0)));
      });
      List<Showtime> shows = sessionFactory.fromTransaction(session ->
          session.createSelectionQuery("from Showtime order by startsAt", Showtime.class).getResultList());
      System.out.println("Showtimes: " + shows);
    }

    System.out.println();
    System.out.println("== 3. Spring Boot 4.1.1 with spring-boot-starter-data-jpa (no SessionFactory bean)");
    try (ConfigurableApplicationContext context = SpringApplication.run(ShowtimeApplication.class)) {
      ShowtimeRepository repository = context.getBean(ShowtimeRepository.class);
      repository.save(new Showtime("Up", 3, LocalDateTime.of(2026, 10, 10, 14, 0)));
      repository.save(new Showtime("Coco", 3, LocalDateTime.of(2026, 10, 10, 11, 0)));
      System.out.println("Screen 3: " + repository.findByScreenOrderByStartsAt(3));

      EntityManagerFactory emf = context.getBean(EntityManagerFactory.class);
      System.out.println("EntityManagerFactory is a SessionFactory: " + (emf instanceof SessionFactory));
      System.out.println("SessionFactory beans: " + Arrays.toString(context.getBeanNamesForType(SessionFactory.class)));
      SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);
      long count = sessionFactory.fromTransaction(session ->
          session.createSelectionQuery("select count(*) from Showtime", Long.class).getSingleResult());
      System.out.println("Count with the unwrapped SessionFactory: " + count);

      ShowtimeDao dao = context.getBean(ShowtimeDao.class);
      dao.save(new Showtime("Up", 1, LocalDateTime.of(2026, 10, 10, 19, 0)));
      System.out.println("DAO with Session: " + dao.findByMovie("Up"));
    }
  }
}
