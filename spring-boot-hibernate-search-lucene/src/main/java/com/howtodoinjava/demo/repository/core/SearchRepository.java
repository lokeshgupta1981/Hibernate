package com.howtodoinjava.demo.repository.core;

import org.hibernate.search.engine.search.sort.dsl.SortOrder;

import java.io.Serializable;
import java.util.List;

public interface SearchRepository<T, ID extends Serializable> {

  List<T> fullTextSearch(String text, int offset, int limit, List<String> fields, String sortBy, SortOrder sortOrder);
  List<T> fuzzySearch(String text, int offset, int limit, List<String> fields, String sortBy, SortOrder sortOrder);
  List<T> wildcardSearch(String pattern, int offset, int limit, List<String> fields, String sortBy, SortOrder sortOrder);
}
