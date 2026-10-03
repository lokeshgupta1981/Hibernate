package com.howtodoinjava.hibernate.equality.naive;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.Objects;

/** Naive id-based equals() and hashCode(): the hash code changes when the id is generated. */
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Member other)) return false;
    return Objects.equals(getId(), other.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId());
  }
}
