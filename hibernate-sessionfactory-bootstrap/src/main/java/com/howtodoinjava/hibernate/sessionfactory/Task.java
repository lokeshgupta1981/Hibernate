package com.howtodoinjava.hibernate.sessionfactory;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.LocalDate;

@Entity
public class Task {

  @Id
  @GeneratedValue
  private Long id;

  private String title;
  private String status;
  private LocalDate dueDate;

  protected Task() {
  }

  public Task(String title, String status, LocalDate dueDate) {
    this.title = title;
    this.status = status;
    this.dueDate = dueDate;
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  @Override
  public String toString() {
    return title + " (" + status + ", due " + dueDate + ")";
  }
}
