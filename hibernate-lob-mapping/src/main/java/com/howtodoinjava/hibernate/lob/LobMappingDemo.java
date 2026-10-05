package com.howtodoinjava.hibernate.lob;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.Arrays;
import org.hibernate.Hibernate;

public class LobMappingDemo {

  static final Path RESUME = Path.of("files/lokesh-resume.pdf");
  static final String COVER_LETTER = "Dear hiring team, I would like to apply for the Java developer role.";

  public static void main(String[] args) throws Exception {
    step("0. DDL generated for MySQL and PostgreSQL (no connection needed)");
    System.out.println(Database.ddlFor("MySQL", 8));
    System.out.println(Database.ddlFor("PostgreSQL", 17));

    step("1. Start Hibernate on H2 and create the tables");
    try (EntityManagerFactory emf = Database.create(true)) {

      step("2. Save a job application with a byte[] resume and a String cover letter");
      byte[] resume = Files.readAllBytes(RESUME);
      System.out.println("resume bytes = " + resume.length);
      Long appId = emf.callInTransaction(em -> {
        JobApplication app = new JobApplication("Lokesh");
        app.setResume(resume);
        app.setCoverLetter(COVER_LETTER);
        em.persist(app);
        return app.getId();
      });

      step("3. Load it and write the resume to a file");
      emf.runInTransaction(em -> {
        JobApplication app = em.find(JobApplication.class, appId);
        System.out.println("resume loaded = " + Persistence.getPersistenceUtil().isLoaded(app, "resume"));
        System.out.println("same bytes = " + Arrays.equals(resume, app.getResume()));
        System.out.println("cover letter = " + app.getCoverLetter());
        try {
          Files.createDirectories(Path.of("target"));
          Files.write(Path.of("target/downloaded-resume.pdf"), app.getResume());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });

      step("4. Stream a 20 MB portfolio into a Blob and the extracted text into a Clob");
      Path portfolio = Samples.portfolio(20 * 1024 * 1024);
      long size = Files.size(portfolio);
      Long fileId = emf.callInTransaction(em -> {
        try (InputStream in = Files.newInputStream(portfolio)) {
          ApplicationFile file = new ApplicationFile("portfolio.zip", size);
          file.setContent(Hibernate.getLobHelper().createBlob(in, size));
          file.setExtractedText(Hibernate.getLobHelper().createClob("Lokesh, Java Developer, Hibernate, Spring Boot"));
          em.persist(file);
          em.flush();                 // insert while the stream is open
          return file.getId();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });

      step("5. Load the file (enhanced entity) and stream the Blob back to disk");
      emf.runInTransaction(em -> {
        ApplicationFile file = em.find(ApplicationFile.class, fileId);
        System.out.println("content loaded = " + Persistence.getPersistenceUtil().isLoaded(file, "content"));
        Path copy = Path.of("target/portfolio-copy.zip");
        try (InputStream in = file.getContent().getBinaryStream()) {
          long copied = Files.copy(in, copy, StandardCopyOption.REPLACE_EXISTING);
          System.out.println("copied bytes = " + copied);
          System.out.println("same file = " + (Files.mismatch(portfolio, copy) == -1));
          Clob text = file.getExtractedText();
          System.out.println("text length = " + text.length());
          System.out.println("first 6 chars = " + text.getSubString(1, 6));
        } catch (IOException | SQLException e) {
          throw new RuntimeException(e);
        }
      });

      step("6. Read the Blob after the transaction ended");
      Blob detached = emf.callInTransaction(em -> em.find(ApplicationFile.class, fileId).getContent());
      try (InputStream in = detached.getBinaryStream()) {
        System.out.println("read after commit = " + in.readAllBytes().length);
      } catch (Exception e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }

      step("7. Save a draft without @Lob");
      try {
        emf.runInTransaction(em -> em.persist(new ApplicationDraft(resume, "short")));
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
        Throwable c = e;
        while (c.getCause() != null) c = c.getCause();
        System.out.println("root: " + c.getClass().getName() + ": " + c.getMessage());
      }

      step("8. Save a job posting with @JdbcTypeCode and @Column(length)");
      Long postingId = emf.callInTransaction(em -> {
        JobPosting posting = new JobPosting("Java Developer");
        posting.setDescription("We build a job portal. ".repeat(100));
        posting.setCompanyLogo(resume);
        em.persist(posting);
        return posting.getId();
      });
      emf.runInTransaction(em -> {
        JobPosting p = em.find(JobPosting.class, postingId);
        System.out.println("description length = " + p.getDescription().length());
      });

      step("9. Heap held by a loaded 20 MB byte[] vs a 20 MB Blob");
      System.out.println(Samples.heapReport(emf, portfolio));
    }
  }

  static void step(String title) {
    System.out.println();
    System.out.println("== " + title + " ==");
  }
}
