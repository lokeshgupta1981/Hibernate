package com.howtodoinjava.hibernate.sorting;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.hibernate.annotations.SQLOrder;
import org.hibernate.annotations.SortComparator;
import org.hibernate.annotations.SortNatural;

@Entity
public class Trail {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;
  private String region;
  private double lengthKm;
  private String difficulty;

  // 1. Sorted by the database: ORDER BY added to the SELECT that loads the collection
  @OneToMany(mappedBy = "trail")
  @OrderBy("rating DESC, author ASC")
  private List<TrailReview> reviews = new ArrayList<>();

  // 2. Same reviews, sorted in memory by a Comparator (no ORDER BY in the SQL)
  @OneToMany(mappedBy = "trail")
  @SortComparator(ReviewByRating.class)
  private SortedSet<TrailReview> sortedReviews = new TreeSet<>(new ReviewByRating());

  // 3. Same reviews, ordered by a native SQL fragment (Hibernate's @SQLOrder)
  @OneToMany(mappedBy = "trail")
  @SQLOrder("lower(author)")
  private List<TrailReview> reviewsByAuthor = new ArrayList<>();

  // 4. Order stored in a position column
  @ElementCollection
  @OrderColumn(name = "position")
  private List<String> waypoints = new ArrayList<>();

  // 5. Sorted in memory by String.compareTo()
  @ElementCollection
  @SortNatural
  private SortedSet<String> tags = new TreeSet<>();

  protected Trail() {
  }

  public Trail(String name, String region, double lengthKm, String difficulty) {
    this.name = name;
    this.region = region;
    this.lengthKm = lengthKm;
    this.difficulty = difficulty;
  }

  public void addReview(TrailReview review) {
    reviews.add(review);
    review.setTrail(this);
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getRegion() {
    return region;
  }

  public double getLengthKm() {
    return lengthKm;
  }

  public String getDifficulty() {
    return difficulty;
  }

  public List<TrailReview> getReviews() {
    return reviews;
  }

  public SortedSet<TrailReview> getSortedReviews() {
    return sortedReviews;
  }

  public List<TrailReview> getReviewsByAuthor() {
    return reviewsByAuthor;
  }

  public List<String> getWaypoints() {
    return waypoints;
  }

  public SortedSet<String> getTags() {
    return tags;
  }

  @Override
  public String toString() {
    return name;
  }
}
