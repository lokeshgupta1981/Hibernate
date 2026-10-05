package com.howtodoinjava.hibernate.equality.lombokfixed;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
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
