package com.howtodoinjava.hibernate.datetime;

import static com.howtodoinjava.hibernate.datetime.DateTimeDemo.JAVA_RECORDS;
import static com.howtodoinjava.hibernate.datetime.DateTimeDemo.SPRING_SECURITY;
import static com.howtodoinjava.hibernate.datetime.DateTimeDemo.VIRTUAL_THREADS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.MappingSettings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DateTimeMappingTest {

  private static TimeZone originalZone;

  private EntityManagerFactory emf;
  private Long recordsId;

  @BeforeAll
  static void serverZone() {
    originalZone = TimeZone.getDefault();
    TimeZone.setDefault(DateTimeDemo.SERVER_ZONE);
  }

  @AfterAll
  static void restoreZone() {
    TimeZone.setDefault(originalZone);
  }

  @BeforeEach
  void setUp() {
    emf = Database.create(false);
    recordsId = emf.callInTransaction(em -> {
      Webinar records = new Webinar("Java Records", JAVA_RECORDS, Duration.ofMinutes(90));
      em.persist(records);
      em.persist(new Webinar("Virtual Threads", VIRTUAL_THREADS, Duration.ofMinutes(60)));
      em.persist(new Webinar("Spring Security", SPRING_SECURITY, Duration.ofMinutes(45)));
      return records.getId();
    });
  }

  @AfterEach
  void tearDown() {
    emf.close();
  }

  private Webinar records() {
    return emf.callInTransaction(em -> em.find(Webinar.class, recordsId));
  }

  @Test
  void javaTimeTypesMapToTheseColumnTypes() {
    assertEquals("DATE", Database.columnType(emf, "Webinar", "eventDate"));
    assertEquals("TIME", Database.columnType(emf, "Webinar", "startTime"));
    assertEquals("TIMESTAMP", Database.columnType(emf, "Webinar", "registrationCloses"));
    assertEquals("TIMESTAMP WITH TIME ZONE", Database.columnType(emf, "Webinar", "startsAt"));
    assertEquals("TIMESTAMP WITH TIME ZONE", Database.columnType(emf, "Webinar", "startWithOffset"));
    assertEquals("TIMESTAMP WITH TIME ZONE", Database.columnType(emf, "Webinar", "startInZone"));
    assertEquals("CHARACTER VARYING", Database.columnType(emf, "Webinar", "hostZone"));
    assertEquals("NUMERIC", Database.columnType(emf, "Webinar", "length"));
    assertEquals("TIMESTAMP WITH TIME ZONE", Database.columnType(emf, "Webinar", "createdOn"));
  }

  @Test
  void otherTypesMapToTheseColumnTypes() {
    assertEquals("INTEGER", Database.columnType(emf, "WebinarSeries", "season"));
    assertEquals("BINARY VARYING", Database.columnType(emf, "WebinarSeries", "launchMonth"));
    assertEquals("TIME WITH TIME ZONE", Database.columnType(emf, "WebinarSeries", "dailyStart"));
    assertEquals("INTERVAL", Database.columnType(emf, "WebinarSeries", "breakLength"));
    assertEquals("TIMESTAMP", Database.columnType(emf, "WebinarSeries", "legacyCreated"));
    assertEquals("DATE", Database.columnType(emf, "WebinarSeries", "legacyLaunchDay"));
  }

  @Test
  void storedValuesForJavaRecords() {
    List<String> stored = Database.raw(emf, "Webinar", recordsId, "eventDate", "startTime",
        "registrationCloses", "startsAt", "startWithOffset", "startInZone", "hostZone", "length");
    assertEquals(List.of("2026-11-12", "18:30:00", "2026-11-11 18:30:00", "2026-11-12 13:00:00+00",
        "2026-11-12 18:30:00+05:30", "2026-11-12 18:30:00+05:30", "Asia/Kolkata", "5400000000000"),
        stored);
  }

  @Test
  void valuesComeBackLikeThis() {
    Webinar w = records();
    assertEquals(LocalDate.of(2026, 11, 12), w.getEventDate());
    assertEquals(LocalTime.of(18, 30), w.getStartTime());
    assertEquals(LocalDateTime.of(2026, 11, 11, 18, 30), w.getRegistrationCloses());
    assertEquals(Instant.parse("2026-11-12T13:00:00Z"), w.getStartsAt());
    assertEquals(OffsetDateTime.parse("2026-11-12T18:30+05:30"), w.getStartWithOffset());
    assertEquals(Duration.ofMinutes(90), w.getLength());
    assertEquals(ZoneId.of("Asia/Kolkata"), w.getHostZone());
  }

  @Test
  void zonedDateTimeLosesTheRegionButKeepsTheOffset() {
    ZonedDateTime back = records().getStartInZone();
    assertEquals("2026-11-12T18:30+05:30", back.toString());
    assertEquals(ZoneOffset.ofHoursMinutes(5, 30), back.getZone());
    assertFalse(back.equals(JAVA_RECORDS));
    assertTrue(back.isEqual(JAVA_RECORDS));
    // the region comes back from the separate hostZone column
    Webinar w = records();
    assertEquals(JAVA_RECORDS, w.getStartInZone().withZoneSameInstant(w.getHostZone()));
  }

  @Test
  void eachTimeZoneStorageTypeStoresThis() {
    Long id = emf.callInTransaction(em -> {
      Broadcast b = new Broadcast(JAVA_RECORDS);
      em.persist(b);
      return b.getId();
    });
    assertEquals(List.of("2026-11-12 18:30:00+05:30", "2026-11-12 18:30:00+05:30",
        "2026-11-12 08:00:00", "2026-11-12 13:00:00+00", "2026-11-12 13:00:00+00", "19800"),
        Database.raw(emf, "Broadcast", id, "defaultStart", "nativeStart", "normalizedStart",
            "utcStart", "columnStart", "columnStart_offset"));
    assertEquals("TIMESTAMP", Database.columnType(emf, "Broadcast", "normalizedStart"));
    assertEquals("INTEGER", Database.columnType(emf, "Broadcast", "columnStart_offset"));

    Broadcast b = emf.callInTransaction(em -> em.find(Broadcast.class, id));
    assertEquals("2026-11-12T18:30+05:30", b.getDefaultStart().toString());
    assertEquals("2026-11-12T18:30+05:30", b.getNativeStart().toString());
    assertEquals("2026-11-12T08:00-05:00[America/New_York]", b.getNormalizedStart().toString());
    assertEquals("2026-11-12T13:00Z", b.getUtcStart().toString());
    assertEquals("2026-11-12T18:30+05:30", b.getColumnStart().toString());
    // all five are the same instant
    for (ZonedDateTime z : List.of(b.getDefaultStart(), b.getNativeStart(), b.getNormalizedStart(),
        b.getUtcStart(), b.getColumnStart())) {
      assertTrue(z.isEqual(JAVA_RECORDS));
    }
  }

  @Test
  void creationAndUpdateTimestampsAreSetOnInsert() {
    Webinar w = records();
    assertNotNull(w.getCreatedOn());
    assertNotNull(w.getUpdatedOn());
    assertFalse(w.getUpdatedOn().isBefore(w.getCreatedOn()));
    assertTrue(Duration.between(w.getCreatedOn(), Instant.now()).abs().toMinutes() < 1);
  }

  @Test
  void updateTimestampChangesOnUpdate() {
    Webinar before = records();
    emf.runInTransaction(em -> em.find(Webinar.class, recordsId).setTitle("Java Records in Practice"));
    Webinar after = records();
    assertEquals(before.getCreatedOn(), after.getCreatedOn());
    assertTrue(after.getUpdatedOn().isAfter(before.getUpdatedOn()));
  }

  @Test
  void creationTimestampColumnsAreNotNull() {
    String nullable = emf.callInTransaction(em -> (String) em.createNativeQuery("""
        select is_nullable from information_schema.columns
        where table_name = 'WEBINAR' and column_name = 'CREATEDON'""").getSingleResult());
    assertEquals("NO", nullable);
  }

  @Test
  void databaseClockFillsCreatedOnAtInsert() {
    WebinarSeries saved = emf.callInTransaction(em -> {
      WebinarSeries series = new WebinarSeries("Modern Java", Year.of(2026), YearMonth.of(2026, 11),
          OffsetTime.of(18, 30, 0, 0, ZoneOffset.ofHoursMinutes(5, 30)), Duration.ofMinutes(10));
      em.persist(series);
      em.flush();
      assertNotNull(series.getCreatedOn());
      return series;
    });
    assertTrue(Duration.between(saved.getCreatedOn(), Instant.now()).abs().toMinutes() < 1);

    WebinarSeries back = emf.callInTransaction(em -> em.find(WebinarSeries.class, saved.getId()));
    assertEquals(Year.of(2026), back.getSeason());
    assertEquals(YearMonth.of(2026, 11), back.getLaunchMonth());
    assertEquals(OffsetTime.parse("18:30+05:30"), back.getDailyStart());
    assertEquals(Duration.ofMinutes(10), back.getBreakLength());
    assertEquals(List.of("2026", "18:30:00+05:30", "INTERVAL '600' SECOND"),
        Database.raw(emf, "WebinarSeries", saved.getId(), "season", "dailyStart", "breakLength"));
  }

  @Test
  void legacyDateAndCalendarStillWork() {
    Date created = Date.from(Instant.parse("2026-11-01T10:15:30Z"));
    Calendar launch = Calendar.getInstance();
    launch.clear();
    launch.set(2026, Calendar.NOVEMBER, 12);
    Long id = emf.callInTransaction(em -> {
      WebinarSeries series = new WebinarSeries("Modern Java", Year.of(2026), YearMonth.of(2026, 11),
          OffsetTime.parse("18:30+05:30"), Duration.ofMinutes(10));
      series.setLegacyCreated(created);
      series.setLegacyLaunchDay(launch);
      em.persist(series);
      return series.getId();
    });
    WebinarSeries back = emf.callInTransaction(em -> em.find(WebinarSeries.class, id));
    assertEquals(created.getTime(), back.getLegacyCreated().getTime());
    assertEquals(launch.getTimeInMillis(), back.getLegacyLaunchDay().getTimeInMillis());
  }

  @Test
  void localTimeIsRoundedToWholeSecondsOnH2() {
    Long id = emf.callInTransaction(em -> {
      Webinar w = new Webinar("Java Records", JAVA_RECORDS.withSecond(15).withNano(500_000_000),
          Duration.ofMinutes(90));
      em.persist(w);
      return w.getId();
    });
    Webinar back = emf.callInTransaction(em -> em.find(Webinar.class, id));
    assertEquals(LocalTime.of(18, 30, 16), back.getStartTime());
    assertEquals(LocalDateTime.of(2026, 11, 11, 18, 30, 15, 500_000_000), back.getRegistrationCloses());
  }

  @Test
  void betweenOnLocalDate() {
    List<String> titles = emf.callInTransaction(em -> em.createQuery(
            "select w.title from Webinar w where w.eventDate between :from and :to order by w.eventDate",
            String.class)
        .setParameter("from", LocalDate.of(2026, 11, 1))
        .setParameter("to", LocalDate.of(2026, 11, 30))
        .getResultList());
    assertEquals(List.of("Java Records", "Virtual Threads"), titles);
  }

  @Test
  void halfOpenRangeOnInstant() {
    List<String> titles = emf.callInTransaction(em -> em.createQuery(
            "select w.title from Webinar w where w.startsAt >= :from and w.startsAt < :to", String.class)
        .setParameter("from", Instant.parse("2026-11-12T00:00:00Z"))
        .setParameter("to", Instant.parse("2026-11-13T00:00:00Z"))
        .getResultList());
    assertEquals(List.of("Java Records"), titles);
  }

  @Test
  void halfOpenRangeOnLocalDateTimeForOneDay() {
    LocalDate day = LocalDate.of(2026, 11, 11);
    List<String> titles = emf.callInTransaction(em -> em.createQuery(
            "select w.title from Webinar w where w.registrationCloses >= :from and w.registrationCloses < :to",
            String.class)
        .setParameter("from", day.atStartOfDay())
        .setParameter("to", day.plusDays(1).atStartOfDay())
        .getResultList());
    assertEquals(List.of("Java Records"), titles);
  }

  @Test
  void compareDuration() {
    List<String> titles = emf.callInTransaction(em -> em.createQuery(
            "select w.title from Webinar w where w.length >= :min order by w.length desc", String.class)
        .setParameter("min", Duration.ofMinutes(60))
        .getResultList());
    assertEquals(List.of("Java Records", "Virtual Threads"), titles);
  }

  @Test
  void extractYearAndMonth() {
    List<Object[]> rows = emf.callInTransaction(em -> em.createQuery(
            "select extract(month from w.eventDate), count(w) from Webinar w"
                + " where extract(year from w.eventDate) = 2026"
                + " group by extract(month from w.eventDate) order by 1", Object[].class)
        .getResultList());
    assertEquals(2, rows.size());
    assertEquals(11, ((Number) rows.get(0)[0]).intValue());
    assertEquals(2L, rows.get(0)[1]);
    assertEquals(12, ((Number) rows.get(1)[0]).intValue());
    assertEquals(1L, rows.get(1)[1]);
  }

  @Test
  void localDateInJpqlIsToday() {
    List<String> titles = emf.callInTransaction(em -> em.createQuery(
            "select w.title from Webinar w where w.eventDate >= local date order by w.eventDate", String.class)
        .getResultList());
    List<String> expected = List.of(JAVA_RECORDS, VIRTUAL_THREADS, SPRING_SECURITY).stream()
        .filter(z -> !z.toLocalDate().isBefore(LocalDate.now()))
        .map(z -> z == JAVA_RECORDS ? "Java Records" : z == VIRTUAL_THREADS ? "Virtual Threads" : "Spring Security")
        .toList();
    assertEquals(expected, titles);
  }

  @Test
  void jdbcTimeZoneUtcShiftsLocalDateTimeInTheColumn() {
    try (EntityManagerFactory utc = Database.create("webinars_utc_test", false, Map.of(JdbcSettings.JDBC_TIME_ZONE, "UTC"))) {
      Long id = utc.callInTransaction(em -> {
        Webinar w = new Webinar("Java Records", JAVA_RECORDS, Duration.ofMinutes(90));
        em.persist(w);
        return w.getId();
      });
      assertEquals(List.of("2026-11-11 23:30:00", "2026-11-12 13:00:00+00"),
          Database.raw(utc, "Webinar", id, "registrationCloses", "startsAt"));
      assertEquals(LocalDateTime.of(2026, 11, 11, 18, 30),
          utc.callInTransaction(em -> em.find(Webinar.class, id)).getRegistrationCloses());
    }
  }

  @Test
  void defaultStorageSettingAppliesToAllZonedFields() {
    try (EntityManagerFactory utc = Database.create("webinars_storage_test", false,
        Map.of(MappingSettings.TIMEZONE_DEFAULT_STORAGE, "NORMALIZE_UTC"))) {
      Long id = utc.callInTransaction(em -> {
        Webinar w = new Webinar("Java Records", JAVA_RECORDS, Duration.ofMinutes(90));
        em.persist(w);
        return w.getId();
      });
      assertEquals(List.of("2026-11-12 13:00:00+00", "2026-11-12 13:00:00+00"),
          Database.raw(utc, "Webinar", id, "startWithOffset", "startInZone"));
      Webinar back = utc.callInTransaction(em -> em.find(Webinar.class, id));
      assertEquals("2026-11-12T13:00Z", back.getStartInZone().toString());
      assertEquals("2026-11-12T13:00Z", back.getStartWithOffset().toString());
    }
  }

  @Test
  void yearMonthWithConverterIsStoredAsDate() {
    Long id = emf.callInTransaction(em -> {
      WebinarSeries series = new WebinarSeries("Modern Java", Year.of(2026), YearMonth.of(2026, 11),
          OffsetTime.parse("18:30+05:30"), Duration.ofMinutes(10));
      series.setEndMonth(YearMonth.of(2027, 3));
      em.persist(series);
      return series.getId();
    });
    assertEquals("DATE", Database.columnType(emf, "WebinarSeries", "endMonth"));
    assertEquals(List.of("2027-03-01"), Database.raw(emf, "WebinarSeries", id, "endMonth"));
    assertEquals(YearMonth.of(2027, 3),
        emf.callInTransaction(em -> em.find(WebinarSeries.class, id)).getEndMonth());
  }

  @Test
  void secondPrecisionKeepsMilliseconds() {
    Long id = emf.callInTransaction(em -> {
      WebinarSeries series = new WebinarSeries("Modern Java", Year.of(2026), YearMonth.of(2026, 11),
          OffsetTime.parse("18:30+05:30"), Duration.ofMinutes(10));
      series.setReminderTime(LocalTime.of(18, 30, 15, 500_000_000));
      em.persist(series);
      return series.getId();
    });
    Number precision = emf.callInTransaction(em -> (Number) em.createNativeQuery("""
        select datetime_precision from information_schema.columns
        where table_name = 'WEBINARSERIES' and column_name = 'REMINDERTIME'""").getSingleResult());
    assertEquals(3, precision.intValue());
    assertEquals(LocalTime.of(18, 30, 15, 500_000_000),
        emf.callInTransaction(em -> em.find(WebinarSeries.class, id)).getReminderTime());
  }
}
