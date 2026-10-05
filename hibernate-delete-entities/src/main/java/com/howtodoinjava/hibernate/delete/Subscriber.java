package com.howtodoinjava.hibernate.delete;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreRemove;

@Entity
public class Subscriber {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  private boolean confirmed;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "newsletter_id")
  private Newsletter newsletter;

  protected Subscriber() {
  }

  public Subscriber(String name, boolean confirmed) {
    this.name = name;
    this.confirmed = confirmed;
  }

  @PreRemove
  void beforeRemove() {
    System.out.println("@PreRemove " + name);
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public boolean isConfirmed() {
    return confirmed;
  }

  public Newsletter getNewsletter() {
    return newsletter;
  }

  void setNewsletter(Newsletter newsletter) {
    this.newsletter = newsletter;
  }

  @Override
  public String toString() {
    return name;
  }
}
