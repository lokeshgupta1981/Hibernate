package com.howtodoinjava.hibernate.lob;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Large text and binary columns without @Lob. */
@Entity
public class JobPosting {

  @Id
  @GeneratedValue
  private Long id;

  private String title;

  @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
  private String description;

  @Column(length = 5_000_000)
  private byte[] companyLogo;

  protected JobPosting() {
  }

  public JobPosting(String title) {
    this.title = title;
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public byte[] getCompanyLogo() {
    return companyLogo;
  }

  public void setCompanyLogo(byte[] companyLogo) {
    this.companyLogo = companyLogo;
  }
}
