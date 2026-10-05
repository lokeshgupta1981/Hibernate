package com.howtodoinjava.hibernate.annotations;

import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class AnnotationsDemo {

  public static void main(String[] args) throws Exception {
    Path ddl = Files.createTempFile("theater-ddl", ".sql");
    Files.delete(ddl);
    try (EntityManagerFactory emf = Database.create(true, ddl)) {

      step("1. DDL generated from the annotations");
      System.out.println(Files.readString(ddl));

      step("2. Persist a theater (IDENTITY), a play (SEQUENCE) and a performance (UUID)");
      Object[] ids = emf.callInTransaction(em -> {
        Theater theater = new Theater("Globe", 300,
            new Address("21 New Globe Walk", "London", "SE1 9DT"),
            new BoxOffice("020 7401 9919", "box@globe.example"));
        em.persist(theater);

        Play play = new Play("Hamlet", Genre.DRAMA, 180, new BigDecimal("40.00"));
        play.setSynopsis("The prince of Denmark avenges his father.");
        play.setIntermission(true);
        play.setFeatured(true);
        em.persist(play);

        Performance evening = new Performance(play, theater,
            LocalDateTime.of(2026, 11, 7, 19, 30), "Main hall");
        em.persist(evening);
        return new Object[] {theater.getId(), play.getId(), evening.getId()};
      });
      Long theaterId = (Long) ids[0];
      Long playId = (Long) ids[1];
      UUID performanceId = (UUID) ids[2];
      System.out.println("theater id = " + theaterId + ", play id = " + playId
          + ", performance id = " + performanceId);

      step("3. Raw column values");
      emf.runInTransaction(em -> {
        Object[] p = (Object[]) em.createNativeQuery(
                "select genre, intermission, slug, version, ticket_price from play")
            .getSingleResult();
        System.out.println("play: genre=" + p[0] + ", intermission=" + p[1] + ", slug=" + p[2]
            + ", version=" + p[3] + ", ticket_price=" + p[4]);
        Object[] s = (Object[]) em.createNativeQuery(
                "select status, language, play_id from performance")
            .getSingleResult();
        System.out.println("performance: status=" + s[0] + ", language=" + s[1]
            + ", play_id=" + s[2]);
        Object[] t = (Object[]) em.createNativeQuery(
                "select name, street, city, postal_code, phone, email from Theater")
            .getSingleResult();
        System.out.println("theater: " + java.util.Arrays.toString(t));
      });

      step("4. Load the play: @Transient field and @Lob with @Basic(fetch = LAZY)");
      emf.runInTransaction(em -> {
        Play play = em.find(Play.class, playId);
        System.out.println("featured = " + play.isFeatured() + ", intermission = "
            + play.isIntermission() + ", synopsis loaded = "
            + org.hibernate.Hibernate.isPropertyInitialized(play, "synopsis"));
      });

      step("5. Update the play: @Version");
      emf.runInTransaction(em -> em.find(Play.class, playId).setTicketPrice(new BigDecimal("45.00")));
      System.out.println("version = " + emf.callInTransaction(
          em -> em.find(Play.class, playId).getVersion()));

      step("6. JPQL uses the entity name Show, the table is performance");
      List<String> halls = emf.callInTransaction(em ->
          em.createQuery("select s.hall from Show s where s.playId = :id", String.class)
              .setParameter("id", playId)
              .getResultList());
      System.out.println("halls = " + halls);
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("=== " + title + " ===");
  }
}
