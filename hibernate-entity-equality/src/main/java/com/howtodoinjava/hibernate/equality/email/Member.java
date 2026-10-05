package com.howtodoinjava.hibernate.equality.email;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.Objects;
import org.hibernate.annotations.NaturalId;

/** Business key: the membership email identifies a member, before and after persist. */
@Entity
public class Member {

  @Id
  @GeneratedValue
  private Long id;

  @NaturalId
  @Column(nullable = false)
  private String email;

  private String name;

  protected Member() {
  }

  public Member(String email, String name) {
    this.email = Objects.requireNonNull(email);
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Member other)) return false;
    return getEmail().equals(other.getEmail());
  }

  @Override
  public int hashCode() {
    return getEmail().hashCode();
  }

  @Override
  public String toString() {
    return "Member(" + email + ")";
  }
}
