package com.howtodoinjava.hibernate.equality.byid;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/** Id-based equals() that treats a null id as "not equal", and a hash code that never changes. */
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
    return getId() != null && getId().equals(other.getId());
  }

  @Override
  public int hashCode() {
    return Member.class.hashCode();
  }
}
