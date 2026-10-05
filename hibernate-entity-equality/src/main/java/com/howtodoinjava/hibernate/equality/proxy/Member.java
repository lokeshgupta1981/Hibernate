package com.howtodoinjava.hibernate.equality.proxy;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.Objects;

/** The equals() an IDE generates: getClass() and direct field access. It fails for Hibernate proxies. */
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
    if (o == null || getClass() != o.getClass()) return false;
    Member other = (Member) o;
    return id != null && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return Member.class.hashCode();
  }
}
