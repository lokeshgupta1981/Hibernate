package com.howtodoinjava.hibernate.equality.lombok;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Lombok @Data on an entity: equals(), hashCode() and toString() use every field. */
@Entity
@Data
@NoArgsConstructor
public class Member {

  @Id
  @GeneratedValue
  private Long id;

  private String email;
  private String name;

  @OneToMany(mappedBy = "member")
  private List<Visit> visits = new ArrayList<>();

  public Member(String email, String name) {
    this.email = email;
    this.name = name;
  }

  public void addVisit(Visit visit) {
    visits.add(visit);
    visit.setMember(this);
  }
}
