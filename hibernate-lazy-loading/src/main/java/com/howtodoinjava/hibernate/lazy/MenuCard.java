package com.howtodoinjava.hibernate.lazy;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

/**
 * The printable menu of a restaurant. This class is bytecode enhanced by the
 * hibernate-maven-plugin (see pom.xml), so its LAZY text loads on first access.
 */
@Entity
public class MenuCard {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String title;

  @Lob
  @Basic(fetch = FetchType.LAZY)
  private String text;

  protected MenuCard() {
  }

  public MenuCard(String title, String text) {
    this.title = title;
    this.text = text;
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getText() {
    return text;
  }
}
