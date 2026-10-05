package com.howtodoinjava.hibernate.ehcache;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Airline {

  @Id
  @GeneratedValue
  private Long id;

  private String name;

  @ManyToMany
  @JoinTable(name = "airline_hub",
      joinColumns = @JoinColumn(name = "airline_id"),
      inverseJoinColumns = @JoinColumn(name = "airport_code"))
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private Set<Airport> hubs = new HashSet<>();

  protected Airline() {
  }

  public Airline(String name) {
    this.name = name;
  }

  public void addHub(Airport airport) {
    hubs.add(airport);
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Set<Airport> getHubs() {
    return hubs;
  }

  /** Hub cities in alphabetical order, for printing and tests. */
  public Set<String> hubCities() {
    Set<String> cities = new TreeSet<>();
    hubs.forEach(a -> cities.add(a.getCity()));
    return cities;
  }
}
