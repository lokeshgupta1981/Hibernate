package com.howtodoinjava.hibernate.associations;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class MembershipCard {

  @Id
  @GeneratedValue
  private Long id;

  private String plan;

  // One-to-one, owning side: holds the member_id foreign key (default fetch EAGER)
  @OneToOne
  @JoinColumn(name = "member_id")
  private Member member;

  protected MembershipCard() {
  }

  public MembershipCard(String plan) {
    this.plan = plan;
  }

  public Long getId() {
    return id;
  }

  public String getPlan() {
    return plan;
  }

  public Member getMember() {
    return member;
  }

  void setMember(Member member) {
    this.member = member;
  }
}
