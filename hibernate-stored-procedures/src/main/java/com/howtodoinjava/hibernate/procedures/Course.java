package com.howtodoinjava.hibernate.procedures;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureParameter;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "course")
@NamedStoredProcedureQuery(
    name = "Course.findByLevel",
    procedureName = "find_courses_by_level",
    resultClasses = Course.class,
    parameters = @StoredProcedureParameter(name = "p_level", type = String.class, mode = ParameterMode.IN))
public class Course {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String title;

  private String level;

  private BigDecimal price;

  protected Course() {
  }

  public Course(String title, String level, BigDecimal price) {
    this.title = title;
    this.level = level;
    this.price = price;
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getLevel() {
    return level;
  }

  public BigDecimal getPrice() {
    return price;
  }

  @Override
  public String toString() {
    return title + " (" + level + ", " + price + ")";
  }
}
