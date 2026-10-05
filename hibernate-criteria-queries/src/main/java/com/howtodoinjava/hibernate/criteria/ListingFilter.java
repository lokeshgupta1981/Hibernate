package com.howtodoinjava.hibernate.criteria;

import java.math.BigDecimal;

/** Optional search inputs from a search form. A null field means "no filter". */
public record ListingFilter(String city, BigDecimal maxPrice, Integer minBedrooms, ListingStatus status,
    String keyword) {

  public static ListingFilter empty() {
    return new ListingFilter(null, null, null, null, null);
  }
}
