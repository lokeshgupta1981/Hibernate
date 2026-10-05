package com.howtodoinjava.hibernate.lob;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceUtil;
import jakarta.persistence.RollbackException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Blob;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.ReadOnlyMode;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LobMappingTest {

  private static final PersistenceUtil UTIL = Persistence.getPersistenceUtil();
  private EntityManagerFactory emf;
  private byte[] resume;

  @BeforeEach
  void setUp() throws IOException {
    emf = Database.create(false);
    resume = Files.readAllBytes(LobMappingDemo.RESUME);
  }

  @AfterEach
  void tearDown() {
    emf.close();
  }

  private String columnType(String table, String column) {
    return emf.callInTransaction(em -> {
      Object[] row = (Object[]) em.createNativeQuery(
              "select data_type, character_maximum_length from information_schema.columns "
                  + "where table_name = ?1 and column_name = ?2")
          .setParameter(1, table.toUpperCase())
          .setParameter(2, column.toUpperCase())
          .getSingleResult();
      return row[0] + (row[1] == null || ((Number) row[1]).longValue() > 1_000_000 ? "" : "(" + row[1] + ")");
    });
  }

  private Long saveApplication() {
    return emf.callInTransaction(em -> {
      JobApplication app = new JobApplication("Lokesh");
      app.setResume(resume);
      app.setCoverLetter(LobMappingDemo.COVER_LETTER);
      em.persist(app);
      return app.getId();
    });
  }

  private Long saveFile(Path path) {
    return emf.callInTransaction(em -> {
      try (InputStream in = Files.newInputStream(path)) {
        long size = Files.size(path);
        ApplicationFile file = new ApplicationFile("portfolio.zip", size);
        file.setContent(Hibernate.getLobHelper().createBlob(in, size));
        file.setExtractedText(Hibernate.getLobHelper().createClob("Lokesh, Java Developer, Hibernate, Spring Boot"));
        em.persist(file);
        em.flush();
        return file.getId();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    });
  }

  @Test
  void lobColumnsOnH2() {
    assertEquals("BINARY LARGE OBJECT", columnType("JobApplication", "resume"));
    assertEquals("CHARACTER LARGE OBJECT", columnType("JobApplication", "coverLetter"));
    assertEquals("BINARY LARGE OBJECT", columnType("ApplicationFile", "content"));
    assertEquals("CHARACTER LARGE OBJECT", columnType("ApplicationFile", "extractedText"));
  }

  @Test
  void defaultColumnsWithoutLobOnH2() {
    assertEquals("BINARY VARYING(255)", columnType("ApplicationDraft", "resume"));
    assertEquals("CHARACTER VARYING(255)", columnType("ApplicationDraft", "coverLetter"));
  }

  @Test
  void jdbcTypeCodeAndLengthOnH2() {
    assertEquals("CHARACTER LARGE OBJECT", columnType("JobPosting", "description"));
    assertEquals("BINARY LARGE OBJECT", columnType("JobPosting", "companyLogo"));
  }

  @Test
  void mysqlDdl() {
    String ddl = Database.ddlFor("MySQL", 8);
    assertTrue(ddl.contains("coverLetter longtext, resume longblob"));
    assertTrue(ddl.contains("content longblob, extractedText longtext"));
    assertTrue(ddl.contains("companyLogo mediumblob, description longtext"));
    assertTrue(ddl.contains("coverLetter varchar(255), resume varbinary(255)"));
  }

  @Test
  void postgresqlDdl() {
    String ddl = Database.ddlFor("PostgreSQL", 17);
    assertTrue(ddl.contains("coverLetter oid, resume oid"));
    assertTrue(ddl.contains("content oid, extractedText oid"));
    assertTrue(ddl.contains("companyLogo bytea, description text"));
    assertTrue(ddl.contains("coverLetter varchar(255), resume bytea"));
  }

  @Test
  void byteArrayAndStringRoundTrip() {
    Long id = saveApplication();
    emf.runInTransaction(em -> {
      JobApplication app = em.find(JobApplication.class, id);
      assertEquals(778, resume.length);
      assertArrayEquals(resume, app.getResume());
      assertEquals(LobMappingDemo.COVER_LETTER, app.getCoverLetter());
    });
  }

  @Test
  void lazyByteArrayIsLoadedWithoutEnhancement() {
    Long id = saveApplication();
    emf.runInTransaction(em -> {
      JobApplication app = em.find(JobApplication.class, id);
      assertTrue(UTIL.isLoaded(app, "resume"));
      assertTrue(UTIL.isLoaded(app, "coverLetter"));
    });
  }

  @Test
  void lazyBlobIsNotLoadedWithEnhancement() throws Exception {
    Long id = saveFile(Samples.portfolio(1024 * 1024));
    emf.runInTransaction(em -> {
      ApplicationFile file = em.find(ApplicationFile.class, id);
      assertFalse(UTIL.isLoaded(file, "content"));
      assertFalse(UTIL.isLoaded(file, "extractedText"));
      assertEquals("portfolio.zip", file.getFileName());
      file.getContent();
      assertTrue(UTIL.isLoaded(file, "content"));
      assertTrue(UTIL.isLoaded(file, "extractedText"));   // same lazy group
    });
  }

  @Test
  void blobStreamsBackToDisk() throws Exception {
    Path portfolio = Samples.portfolio(2 * 1024 * 1024);
    Long id = saveFile(portfolio);
    Path copy = Path.of("target/portfolio-test-copy.zip");
    emf.runInTransaction(em -> {
      ApplicationFile file = em.find(ApplicationFile.class, id);
      try (InputStream in = file.getContent().getBinaryStream()) {
        Files.deleteIfExists(copy);
        assertEquals(2 * 1024 * 1024, Files.copy(in, copy));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    assertEquals(-1, Files.mismatch(portfolio, copy));
  }

  @Test
  void clobReadsText() {
    Long id = saveFile(Samples.portfolio(1024));
    emf.runInTransaction(em -> {
      try {
        var text = em.find(ApplicationFile.class, id).getExtractedText();
        assertEquals(46, text.length());
        assertEquals("Lokesh", text.getSubString(1, 6));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  void closingTheStreamBeforeCommitFails() {
    Path portfolio = Samples.portfolio(1024);
    RollbackException e = assertThrows(RollbackException.class, () -> emf.runInTransaction(em -> {
      try (InputStream in = Files.newInputStream(portfolio)) {
        ApplicationFile file = new ApplicationFile("portfolio.zip", 1024);
        file.setContent(Hibernate.getLobHelper().createBlob(in, 1024));
        em.persist(file);
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    }));
    assertTrue(e.getMessage().contains("Unable to bind parameter #1"));
    assertTrue(e.getMessage().contains("ClosedChannelException"));
  }

  @Test
  void blobIsReadableAfterCommitOnH2() throws Exception {
    Long id = saveFile(Samples.portfolio(1024));
    Blob blob = emf.callInTransaction(em -> em.find(ApplicationFile.class, id).getContent());
    try (InputStream in = blob.getBinaryStream()) {
      assertEquals(1024, in.readAllBytes().length);
    }
  }

  @Test
  void resumeTooLongWithoutLob() {
    RollbackException e = assertThrows(RollbackException.class,
        () -> emf.runInTransaction(em -> em.persist(new ApplicationDraft(resume, "short"))));
    assertTrue(e.getMessage().contains("Value too long for column \"RESUME BINARY VARYING(255)\""));
  }

  @Test
  void coverLetterTooLongWithoutLob() {
    String letter = "I like Java. ".repeat(20);   // 260 characters
    assertEquals(260, letter.length());
    RollbackException e = assertThrows(RollbackException.class,
        () -> emf.runInTransaction(em -> em.persist(new ApplicationDraft(new byte[10], letter))));
    assertTrue(e.getMessage().contains("Value too long for column \"COVERLETTER CHARACTER VARYING(255)\""));
  }

  @Test
  void jobPostingStoresLargeValuesWithoutLob() {
    Long id = emf.callInTransaction(em -> {
      JobPosting posting = new JobPosting("Java Developer");
      posting.setDescription("We build a job portal. ".repeat(100));
      posting.setCompanyLogo(resume);
      em.persist(posting);
      return posting.getId();
    });
    emf.runInTransaction(em -> {
      JobPosting posting = em.find(JobPosting.class, id);
      assertEquals(2300, posting.getDescription().length());
      assertArrayEquals(resume, posting.getCompanyLogo());
    });
  }

  @Test
  void byteArrayKeepsTwoCopiesInHeap() {
    Path portfolio = Samples.portfolio(20 * 1024 * 1024);
    long[] d = Samples.heapDeltas(emf, portfolio);
    long size = 20L * 1024 * 1024;
    assertTrue(d[0] > 1.8 * size, "managed byte[] " + d[0]);
    assertTrue(d[2] > 0.9 * size && d[2] < 1.3 * size, "read-only byte[] " + d[2]);
    assertTrue(d[1] < 0.1 * size, "blob " + d[1]);
  }

  @Test
  void sessionGetLobHelperIsDeprecated() throws Exception {
    assertTrue(Session.class.getMethod("getLobHelper").isAnnotationPresent(Deprecated.class));
    assertFalse(Hibernate.class.getMethod("getLobHelper").isAnnotationPresent(Deprecated.class));
  }

  @Test
  void sessionSaveIsRemoved() {
    assertTrue(java.util.Arrays.stream(Session.class.getMethods()).noneMatch(m -> m.getName().equals("save")));
  }

  @Test
  void lazyBlobAfterTransactionThrows() {
    Long id = saveFile(Samples.portfolio(1024));
    ApplicationFile file = emf.callInTransaction(em -> em.find(ApplicationFile.class, id));
    assertThrows(LazyInitializationException.class, file::getContent);
  }

  @Test
  void readOnlyFindWorks() {
    Long id = saveApplication();
    emf.runInTransaction(em -> assertArrayEquals(resume,
        em.find(JobApplication.class, id, ReadOnlyMode.READ_ONLY).getResume()));
  }
}
