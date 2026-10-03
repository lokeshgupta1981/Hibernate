package com.howtodoinjava.hibernate.criteria;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Listing {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String title;
  private BigDecimal price;
  private int bedrooms;
  private LocalDate listedOn;

  @Enumerated(EnumType.STRING)
  private ListingStatus status;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "neighborhood_id")
  private Neighborhood neighborhood;

  @ManyToMany
  @JoinTable(name = "listing_amenity",
      joinColumns = @JoinColumn(name = "listing_id"),
      inverseJoinColumns = @JoinColumn(name = "amenity_id"))
  private Set<Amenity> amenities = new HashSet<>();

  protected Listing() {
  }

  public Listing(String title, BigDecimal price, int bedrooms, LocalDate listedOn, ListingStatus status,
      Neighborhood neighborhood) {
    this.title = title;
    this.price = price;
    this.bedrooms = bedrooms;
    this.listedOn = listedOn;
    this.status = status;
    this.neighborhood = neighborhood;
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public int getBedrooms() {
    return bedrooms;
  }

  public LocalDate getListedOn() {
    return listedOn;
  }

  public ListingStatus getStatus() {
    return status;
  }

  public Neighborhood getNeighborhood() {
    return neighborhood;
  }

  public Set<Amenity> getAmenities() {
    return amenities;
  }

  @Override
  public String toString() {
    return title;
  }
}
