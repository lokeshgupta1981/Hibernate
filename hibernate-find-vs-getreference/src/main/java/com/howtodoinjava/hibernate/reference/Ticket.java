package com.howtodoinjava.hibernate.reference;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Ticket {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String subject;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assignee_id")
  private Agent assignee;

  protected Ticket() {
  }

  public Ticket(String subject) {
    this.subject = subject;
  }

  public Long getId() {
    return id;
  }

  public String getSubject() {
    return subject;
  }

  public Agent getAssignee() {
    return assignee;
  }

  public void setAssignee(Agent assignee) {
    this.assignee = assignee;
  }
}
