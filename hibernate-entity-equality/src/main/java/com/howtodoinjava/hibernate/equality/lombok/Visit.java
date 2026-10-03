package com.howtodoinjava.hibernate.equality.lombok;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A library visit; @Data here too, so the two classes call each other's hashCode(). */
@Entity
@Data
@NoArgsConstructor
public class Visit {

  @Id
  @GeneratedValue
  private Long id;

  private String weekday;

  @ManyToOne(fetch = FetchType.LAZY)
  private Member member;

  public Visit(String weekday) {
    this.weekday = weekday;
  }
}
