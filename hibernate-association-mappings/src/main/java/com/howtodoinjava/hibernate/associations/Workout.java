package com.howtodoinjava.hibernate.associations;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Workout {

  @Id
  @GeneratedValue
  private Long id;

  private String activity;

  private int minutes;

  // Many-to-one, owning side: holds the member_id foreign key
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id")
  private Member member;

  protected Workout() {
  }

  public Workout(String activity, int minutes) {
    this.activity = activity;
    this.minutes = minutes;
  }

  public Long getId() {
    return id;
  }

  public String getActivity() {
    return activity;
  }

  public int getMinutes() {
    return minutes;
  }

  public Member getMember() {
    return member;
  }

  void setMember(Member member) {
    this.member = member;
  }
}
