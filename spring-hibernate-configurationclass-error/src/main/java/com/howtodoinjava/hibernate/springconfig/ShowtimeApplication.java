package com.howtodoinjava.hibernate.springconfig;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The modern setup: Spring Boot creates the DataSource, the EntityManagerFactory
 * (a Hibernate SessionFactory) and the transaction manager. No SessionFactory bean is declared.
 */
@SpringBootApplication
public class ShowtimeApplication {

  public static void main(String[] args) {
    SpringApplication.run(ShowtimeApplication.class, args);
  }
}
