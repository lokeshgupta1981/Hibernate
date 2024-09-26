package com.howtodoinjava.demo.service;

import com.howtodoinjava.demo.exception.ResourceNotFoundException;
import com.howtodoinjava.demo.model.Book;
import com.howtodoinjava.demo.repository.BookRepository;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class BookService {

  private final BookRepository bookRepository;

  public BookService(BookRepository bookRepository) {
    this.bookRepository = bookRepository;
  }

  public List<Book> findAll() {

    return bookRepository.findAll();
  }

  public Book findById(Long id) throws ResourceNotFoundException {

    return bookRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Book not found for ID :: " + id));
  }

  public Book saveBook(Book book) {

    return bookRepository.save(book);
  }

  public Book updateBook(Long id, Book bookDetails) throws ResourceNotFoundException {

    Book existingBook = bookRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Book not found for ID :: " + id));

    existingBook.setTitle(bookDetails.getTitle());
    existingBook.setAuthor(bookDetails.getAuthor());
    return bookRepository.save(existingBook);
  }

  public void deleteBook(Long id) {
    bookRepository.deleteById(id);
  }

  @Transactional(readOnly = true)
  public List<Book> fullTextSearchByTitle(String title) {

    return bookRepository
        .fullTextSearch(title, 0, 10, List.of("title"),"sort_title", SortOrder.ASC);
  }
}
