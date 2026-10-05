package com.howtodoinjava.hibernate.notfound;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** Fixed mapping: LAZY association with a real foreign key constraint. */
@Entity(name = "ScholarshipApplication")
@Table(name = "scholarship_application")
public class CheckedApplication extends BaseApplication {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "scholarship_id")
  private Scholarship scholarship;

  protected CheckedApplication() {
  }

  public CheckedApplication(String studentName, LocalDate submittedOn, Scholarship scholarship) {
    super(studentName, submittedOn);
    this.scholarship = scholarship;
  }

  @Override
  public Scholarship getScholarship() {
    return scholarship;
  }
}
