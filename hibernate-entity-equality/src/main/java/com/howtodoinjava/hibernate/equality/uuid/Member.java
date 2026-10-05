package com.howtodoinjava.hibernate.equality.uuid;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;

/** The id is a UUID assigned in Java when the object is created, so it is never null. */
@Entity
public class Member {

  @Id
  private UUID id = UUID.randomUUID();

  private String email;
  private String name;

  protected Member() {
  }

  public Member(String email, String name) {
    this.email = email;
    this.name = name;
  }

  public UUID getId() {
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
    return getId().equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getId().hashCode();
  }
}
