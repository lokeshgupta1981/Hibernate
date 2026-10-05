package com.howtodoinjava.hibernate.sorting;

import java.util.Comparator;

/**
 * Highest rating first, then author name. The author breaks ties: a TreeSet
 * drops an element that compares equal to one it already holds.
 */
public class ReviewByRating implements Comparator<TrailReview> {

  @Override
  public int compare(TrailReview a, TrailReview b) {
    return Comparator.comparingInt(TrailReview::getRating).reversed()
        .thenComparing(TrailReview::getAuthor)
        .compare(a, b);
  }
}
