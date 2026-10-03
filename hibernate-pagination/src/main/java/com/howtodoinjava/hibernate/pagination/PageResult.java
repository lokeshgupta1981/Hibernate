package com.howtodoinjava.hibernate.pagination;

import java.util.List;

/** One page of results plus what a pager UI needs. Page numbers start at 1. */
public record PageResult<T>(List<T> content, int pageNumber, int pageSize, long totalElements) {

  public int totalPages() {
    return (int) Math.ceilDiv(totalElements, (long) pageSize);
  }

  public boolean hasNext() {
    return pageNumber < totalPages();
  }
}
