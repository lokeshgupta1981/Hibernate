package com.howtodoinjava.hibernate.associations;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import java.util.HashSet;
import java.util.Set;

@Entity
public class FitnessClass {

  @Id
  @GeneratedValue
  private Long id;

  private String name;

  // Many-to-many, inverse side: Member.classes owns the join table
  @ManyToMany(mappedBy = "classes")
  private Set<Member> members = new HashSet<>();

  protected FitnessClass() {
  }

  public FitnessClass(String name) {
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Set<Member> getMembers() {
    return members;
  }
}
