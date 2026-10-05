package com.howtodoinjava.hibernate.datetime;

import jakarta.persistence.EntityManagerFactory;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.hibernate.cfg.JdbcSettings;

public class DateTimeDemo {

  /** The application server runs in New York; the webinar hosts are elsewhere. */
  public static final TimeZone SERVER_ZONE = TimeZone.getTimeZone("America/New_York");

  public static final ZonedDateTime JAVA_RECORDS =
      ZonedDateTime.of(2026, 11, 12, 18, 30, 0, 0, ZoneId.of("Asia/Kolkata"));
  public static final ZonedDateTime VIRTUAL_THREADS =
      ZonedDateTime.of(2026, 11, 19, 10, 0, 0, 0, ZoneId.of("Europe/London"));
  public static final ZonedDateTime SPRING_SECURITY =
      ZonedDateTime.of(2026, 12, 3, 9, 0, 0, 0, ZoneId.of("America/New_York"));

  public static void main(String[] args) {
    TimeZone.setDefault(SERVER_ZONE);

    try (EntityManagerFactory emf = Database.create(true)) {

      step("1. Column types in the generated DDL");
      for (String column : List.of("eventDate", "startTime", "registrationCloses", "startsAt",
          "startWithOffset", "startInZone", "hostZone", "length", "createdOn")) {
        System.out.println(column + " -> " + Database.columnType(emf, "Webinar", column));
      }
      for (String column : List.of("season", "launchMonth", "endMonth", "dailyStart", "breakLength",
          "legacyCreated", "legacyLaunchDay")) {
        System.out.println(column + " -> " + Database.columnType(emf, "WebinarSeries", column));
      }

      step("2. Save three webinars");
      Long recordsId = emf.callInTransaction(em -> {
        Webinar records = new Webinar("Java Records", JAVA_RECORDS, Duration.ofMinutes(90));
        em.persist(records);
        em.persist(new Webinar("Virtual Threads", VIRTUAL_THREADS, Duration.ofMinutes(60)));
        em.persist(new Webinar("Spring Security", SPRING_SECURITY, Duration.ofMinutes(45)));
        return records.getId();
      });

      step("3. What the database stores for 'Java Records'");
      List<String> stored = Database.raw(emf, "Webinar", recordsId, "eventDate", "startTime",
          "registrationCloses", "startsAt", "startWithOffset", "startInZone", "hostZone", "length");
      System.out.println(stored);

      step("4. What comes back");
      emf.runInTransaction(em -> {
        Webinar w = em.find(Webinar.class, recordsId);
        System.out.println("eventDate=" + w.getEventDate() + " startTime=" + w.getStartTime()
            + " registrationCloses=" + w.getRegistrationCloses());
        System.out.println("startsAt=" + w.getStartsAt() + " startWithOffset=" + w.getStartWithOffset()
            + " startInZone=" + w.getStartInZone() + " hostZone=" + w.getHostZone()
            + " length=" + w.getLength());
        System.out.println("createdOn=" + w.getCreatedOn() + " updatedOn=" + w.getUpdatedOn());
      });

      step("5. One ZonedDateTime stored with each TimeZoneStorageType");
      Long broadcastId = emf.callInTransaction(em -> {
        Broadcast b = new Broadcast(JAVA_RECORDS);
        em.persist(b);
        return b.getId();
      });
      System.out.println("stored: " + Database.raw(emf, "Broadcast", broadcastId, "defaultStart",
          "nativeStart", "normalizedStart", "utcStart", "columnStart", "columnStart_offset"));
      emf.runInTransaction(em -> {
        Broadcast b = em.find(Broadcast.class, broadcastId);
        System.out.println("back: default=" + b.getDefaultStart() + " native=" + b.getNativeStart()
            + " normalized=" + b.getNormalizedStart() + " utc=" + b.getUtcStart()
            + " column=" + b.getColumnStart());
      });

      step("6. Date range queries with JPQL");
      emf.runInTransaction(em -> {
        List<Webinar> november = em.createQuery(
                "from Webinar w where w.eventDate between :from and :to order by w.eventDate", Webinar.class)
            .setParameter("from", LocalDate.of(2026, 11, 1))
            .setParameter("to", LocalDate.of(2026, 11, 30))
            .getResultList();
        System.out.println("November: " + november);

        List<Webinar> onNov12Utc = em.createQuery(
                "from Webinar w where w.startsAt >= :from and w.startsAt < :to", Webinar.class)
            .setParameter("from", Instant.parse("2026-11-12T00:00:00Z"))
            .setParameter("to", Instant.parse("2026-11-13T00:00:00Z"))
            .getResultList();
        System.out.println("Starts on 2026-11-12 UTC: " + onNov12Utc);

        List<Webinar> longOnes = em.createQuery(
                "from Webinar w where w.length >= :min order by w.length desc", Webinar.class)
            .setParameter("min", Duration.ofMinutes(60))
            .getResultList();
        System.out.println("60 minutes or longer: " + longOnes);

        List<Integer> years = em.createQuery(
                "select distinct extract(year from w.eventDate) from Webinar w", Integer.class)
            .getResultList();
        System.out.println("Years: " + years);

        List<Webinar> upcoming = em.createQuery(
                "from Webinar w where w.eventDate >= local date order by w.eventDate", Webinar.class)
            .getResultList();
        System.out.println("Upcoming: " + upcoming);
      });
      step("7. @UpdateTimestamp changes on update, @CreationTimestamp does not");
      Instant[] before = emf.callInTransaction(em -> {
        Webinar w = em.find(Webinar.class, recordsId);
        return new Instant[] {w.getCreatedOn(), w.getUpdatedOn()};
      });
      emf.runInTransaction(em -> em.find(Webinar.class, recordsId).setTitle("Java Records in Practice"));
      emf.runInTransaction(em -> {
        Webinar w = em.find(Webinar.class, recordsId);
        System.out.println("createdOn same=" + w.getCreatedOn().equals(before[0])
            + " updatedOn later=" + w.getUpdatedOn().isAfter(before[1]));
      });

      step("8. Database clock with @CreationTimestamp(source = SourceType.DB)");
      emf.runInTransaction(em -> {
        WebinarSeries series = new WebinarSeries("Modern Java", Year.of(2026), YearMonth.of(2026, 11),
            OffsetTime.of(18, 30, 0, 0, ZoneOffset.ofHoursMinutes(5, 30)), Duration.ofMinutes(10));
        series.setEndMonth(YearMonth.of(2027, 3));
        series.setLegacyCreated(new Date());
        em.persist(series);
        em.flush();
        System.out.println("createdOn after flush=" + (series.getCreatedOn() != null));
      });

    }

    step("9. hibernate.jdbc.time_zone = UTC shifts LocalDateTime in the column");
    try (EntityManagerFactory utc = Database.create("webinars_utc", true, Map.of(JdbcSettings.JDBC_TIME_ZONE, "UTC"))) {
      Long id = utc.callInTransaction(em -> {
        Webinar w = new Webinar("Java Records", JAVA_RECORDS, Duration.ofMinutes(90));
        em.persist(w);
        return w.getId();
      });
      System.out.println("stored: " + Database.raw(utc, "Webinar", id, "registrationCloses", "startsAt"));
      utc.runInTransaction(em -> System.out.println("back: registrationCloses="
          + em.find(Webinar.class, id).getRegistrationCloses()));
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("== " + title + " ==");
  }
}
