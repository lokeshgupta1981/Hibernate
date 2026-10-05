package com.howtodoinjava.hibernate.notfound;

import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** Same legacy table, association fetched EAGER. */
@Entity(name = "ScholarshipApplication")
@Table(name = "scholarship_application")
public class EagerApplication extends BaseApplication {

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "scholarship_id",
      foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
  private Scholarship scholarship;

  protected EagerApplication() {
  }

  public EagerApplication(String studentName, LocalDate submittedOn, Scholarship scholarship) {
    super(studentName, submittedOn);
    this.scholarship = scholarship;
  }

  @Override
  public Scholarship getScholarship() {
    return scholarship;
  }
}
