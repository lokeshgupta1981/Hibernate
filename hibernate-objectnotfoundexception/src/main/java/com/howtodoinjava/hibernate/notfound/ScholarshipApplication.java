package com.howtodoinjava.hibernate.notfound;

import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** Legacy mapping: LAZY association, no foreign key constraint in the table. */
@Entity(name = "ScholarshipApplication")
@Table(name = "scholarship_application")
public class ScholarshipApplication extends BaseApplication {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "scholarship_id",
      foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
  private Scholarship scholarship;

  protected ScholarshipApplication() {
  }

  public ScholarshipApplication(String studentName, LocalDate submittedOn, Scholarship scholarship) {
    super(studentName, submittedOn);
    this.scholarship = scholarship;
  }

  @Override
  public Scholarship getScholarship() {
    return scholarship;
  }
}
