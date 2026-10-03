package com.howtodoinjava.hibernate.cacheprovider;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class PostalCode {

  @Id
  private String code;

  private String town;
  private String district;

  protected PostalCode() {
  }

  public PostalCode(String code, String town, String district) {
    this.code = code;
    this.town = town;
    this.district = district;
  }

  public String getCode() {
    return code;
  }

  public String getTown() {
    return town;
  }

  public String getDistrict() {
    return district;
  }

  @Override
  public String toString() {
    return code + " " + town + " (" + district + ")";
  }
}
