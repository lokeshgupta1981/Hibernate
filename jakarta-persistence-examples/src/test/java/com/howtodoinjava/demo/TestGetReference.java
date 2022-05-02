package com.howtodoinjava.demo;

import com.howtodoinjava.demo.entity.Competition;
import com.howtodoinjava.demo.entity.Nomination;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.*;

public class TestGetReference {
  private static EntityManagerFactory emf;
  private EntityManager em;

  @BeforeAll
  static void setup() {
    emf = Persistence.createEntityManagerFactory("H2DB");
  }

  @AfterAll
  static void tear() {
    emf.close();
  }

  @BeforeEach
  void setupThis() {
    em = emf.createEntityManager();
  }

  @AfterEach
  void tearThis() {
    em.close();
  }

  @Test
  void testGetReference() {

    em.getTransaction().begin();
    Competition competition = new Competition();
    competition.setTitle("Test");
    em.persist(competition);
    em.getTransaction().commit();
    em.close();

    em = emf.createEntityManager();
    em.getTransaction().begin();

    Competition competitionRef = em.getReference(Competition.class, 1L);

    Nomination nomination = new Nomination();
    nomination.setName("Test");
    nomination.setCompetition(competitionRef);

    em.persist(nomination);
    em.getTransaction().commit();
  }
}
