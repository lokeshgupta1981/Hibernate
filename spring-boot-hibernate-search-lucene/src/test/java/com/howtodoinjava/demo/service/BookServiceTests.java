package com.howtodoinjava.demo.service;

import com.howtodoinjava.demo.config.JpaConfig;
import com.howtodoinjava.demo.model.Book;
import com.howtodoinjava.demo.repository.BookRepository;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
public class BookServiceTests {

  @Autowired
  private BookRepository bookRepository;
  private BookService bookService;

  @BeforeEach
  void setUp() {
    bookService = new BookService(bookRepository);
  }

  @Test
  //@Commit
  void testFullTextSearch() {

    bookService.saveBook(new Book(1L, "Harry Potter - Part 1", "Test Author"));
    bookService.saveBook(new Book(2L, "Harry Potter - Part 2", "Test Author"));
    bookService.saveBook(new Book(3L, "Jungle Book - Part 1", "Test Author"));
    bookService.saveBook(new Book(4L, "Jungle Book - Part 2", "Test Author"));

    List<Book> books = bookRepository
        .fullTextSearch("Jungle Book", 0, 10, List.of("title"), "sort_title", SortOrder.DESC);

    assertFalse(books.isEmpty());
    assertEquals(2, books.size());
  }
}
