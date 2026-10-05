package com.howtodoinjava.hibernate.criteria;

import java.util.List;

public record PageResult<T>(List<T> content, int pageNumber, int pageSize, long totalElements) {

  public int totalPages() {
    return (int) Math.ceilDiv(totalElements, (long) pageSize);
  }
}
