package com.howtodoinjava.hibernate.equality.plain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/** No equals() and hashCode(): the Object defaults compare references. */
@Entity
public class Member {

  @Id
  @GeneratedValue
  private Long id;

  private String email;
  private String name;

  protected Member() {
  }

  public Member(String email, String name) {
    this.email = email;
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
