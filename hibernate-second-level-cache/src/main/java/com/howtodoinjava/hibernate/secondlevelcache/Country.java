package com.howtodoinjava.hibernate.secondlevelcache;

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
import java.util.stream.Collectors;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Country {

  @Id
  @GeneratedValue
  private Long id;

  private String name;

  private String capital;

  @ManyToMany
  @JoinTable(name = "country_currency",
      joinColumns = @JoinColumn(name = "country_id"),
      inverseJoinColumns = @JoinColumn(name = "currency_id"))
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private Set<Currency> currencies = new HashSet<>();

  protected Country() {
  }

  public Country(String name, String capital) {
    this.name = name;
    this.capital = capital;
  }

  public void addCurrency(Currency currency) {
    currencies.add(currency);
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getCapital() {
    return capital;
  }

  public void setCapital(String capital) {
    this.capital = capital;
  }

  public Set<Currency> getCurrencies() {
    return currencies;
  }

  public Set<String> currencyNames() {
    return currencies.stream().map(Currency::getName).collect(Collectors.toCollection(TreeSet::new));
  }

  @Override
  public String toString() {
    return name + " (" + capital + ")";
  }
}
