package com.howtodoinjava.hibernate.onetoone.primarykey;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "boarding_pass")
public class BoardingPass {

  // Must be set by hand to the booking id
  @Id
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

  public void setId(Long id) {
    this.id = id;
  }

  public String getSeat() {
    return seat;
  }
}
