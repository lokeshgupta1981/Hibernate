package com.howtodoinjava.hibernate.demo;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;

@SpringBootTest
class HibernateExamplesApplicationTests {

  @Container
  private static final MySQLContainer MYSQL_CONTAINER = (MySQLContainer) new MySQLContainer()
    .withDatabaseName("testdb")
    .withUsername("root")
    .withPassword("password")
    .withInitScript("storedProcedures.sql")
    .withReuse(true);

  @BeforeAll
  static void setup() {
    System.setProperty("MYSQL_HOST", MYSQL_CONTAINER.getHost().toString());
    System.setProperty("MYSQL_PORT", MYSQL_CONTAINER.getFirstMappedPort().toString());
  }

  @Test
  void contextLoads() {
  }
}
