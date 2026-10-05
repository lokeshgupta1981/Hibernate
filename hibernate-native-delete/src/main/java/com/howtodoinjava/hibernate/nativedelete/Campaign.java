package com.howtodoinjava.hibernate.nativedelete;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.SoftDelete;

/** A marketing campaign; deleted campaigns are only marked as deleted. */
@Entity
@Table(name = "campaign")
@SoftDelete
public class Campaign {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  protected Campaign() {
  }

  public Campaign(String name) {
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }
}
