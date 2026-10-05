package com.howtodoinjava.hibernate.notfound;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDate;

/**
 * Columns shared by every variant of the scholarship_application mapping. Each subclass maps the
 * same table and differs only in how it maps the scholarship association.
 */
@MappedSuperclass
public abstract class BaseApplication {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String studentName;

  private LocalDate submittedOn;

  protected BaseApplication() {
  }

  protected BaseApplication(String studentName, LocalDate submittedOn) {
    this.studentName = studentName;
    this.submittedOn = submittedOn;
  }

  public Long getId() {
    return id;
  }

  public String getStudentName() {
    return studentName;
  }

  public void setStudentName(String studentName) {
    this.studentName = studentName;
  }

  public LocalDate getSubmittedOn() {
    return submittedOn;
  }

  public abstract Scholarship getScholarship();
}
