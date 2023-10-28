package com.howtodoinjava.demo;

import com.howtodoinjava.demo.entity.Book;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestLoBData {
  private static EntityManagerFactory emf;
  private static EntityManager em;

  @BeforeAll
  static void setup() {
    emf = Persistence.createEntityManagerFactory("H2DB");
    em = emf.createEntityManager();
    em.getTransaction().begin();
  }

  @AfterAll
  static void tear() {
    em.close();
    emf.close();
  }

  @BeforeEach
  void setupThis() {
    em = emf.createEntityManager();
    em.getTransaction().begin();
  }

  @AfterEach
  void tearThis() {
    em.getTransaction().commit();
    em.close();
  }

  private void flushAndClear() {
    em.flush();
    em.clear();
  }

  @Test
  public void testPersistAndFetchBook() throws IOException, URISyntaxException {
    Book book = new Book();
    book.setName("Hibernate");

    ClassLoader classLoader = getClass().getClassLoader();
    File file = new File(classLoader.getResource("IDEA.png").getFile());
    book.setCover(Files.readAllBytes(file.toPath()));

    em.persist(book);
    flushAndClear();

    Assertions.assertNotNull(book.getId());

    em.detach(book);

    Book bookFetched = em.find(Book.class, book.getId());
    Assertions.assertNotNull(bookFetched.getCover());

    byte[] cover = bookFetched.getCover();

    try (FileOutputStream fos
             = new FileOutputStream("C:\temp\testBook.png")) {
      fos.write(cover);
    } catch (Exception e) {
      e.printStackTrace();
    }

  }
}
