package com.howtodoinjava.hibernate.onetoone.jointable;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "boarding_pass")
public class BoardingPass {

  @Id
  @GeneratedValue
  private Long id;

  private String seat;

  protected BoardingPass() {
  }

  public BoardingPass(String seat) {
    this.seat = seat;
  }

  public Long getId() {
    return id;
  }

  public String getSeat() {
    return seat;
  }
}
