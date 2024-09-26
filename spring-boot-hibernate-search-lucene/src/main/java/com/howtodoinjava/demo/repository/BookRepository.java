package com.howtodoinjava.demo.repository;

import com.howtodoinjava.demo.model.Book;
import com.howtodoinjava.demo.repository.core.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends BaseRepository<Book, Long> {
}