package com.howtodoinjava.hibernate.criteria;

/** One row of the group by query: listings per neighborhood and their average price. */
public record NeighborhoodStats(String neighborhood, Long listings, Double averagePrice) {
}
