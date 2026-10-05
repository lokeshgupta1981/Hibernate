package com.howtodoinjava.hibernate.equality.lombokfixed;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.NaturalId;

/** Lombok without @Data: equals() and hashCode() on the email only, toString() without associations. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Member {

  @Id
  @GeneratedValue
  private Long id;

  @NaturalId
  @Column(nullable = false)
  @EqualsAndHashCode.Include
  @ToString.Include
  private String email;

  @ToString.Include
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
