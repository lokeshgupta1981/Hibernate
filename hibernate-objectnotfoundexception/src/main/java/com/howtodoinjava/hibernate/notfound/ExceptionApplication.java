package com.howtodoinjava.hibernate.notfound;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

/** Legacy table with @NotFound(EXCEPTION): a missing scholarship fails at load time. */
@Entity(name = "ScholarshipApplication")
@Table(name = "scholarship_application")
public class ExceptionApplication extends BaseApplication {

  @ManyToOne(fetch = FetchType.LAZY)
  @NotFound(action = NotFoundAction.EXCEPTION)
  @JoinColumn(name = "scholarship_id")
  private Scholarship scholarship;

  protected ExceptionApplication() {
  }

  public ExceptionApplication(String studentName, LocalDate submittedOn, Scholarship scholarship) {
    super(studentName, submittedOn);
    this.scholarship = scholarship;
  }

  @Override
  public Scholarship getScholarship() {
    return scholarship;
  }
}
