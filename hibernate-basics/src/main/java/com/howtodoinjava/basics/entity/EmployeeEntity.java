package com.howtodoinjava.basics.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;

import java.io.Serial;
import java.io.Serializable;

@Entity
@Table(name = "Employee", uniqueConstraints = {
    @UniqueConstraint(columnNames = "ID"),
    @UniqueConstraint(columnNames = "EMAIL")})
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class EmployeeEntity implements Serializable {

  @Serial
  private static final long serialVersionUID = -1798070786993154676L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID")
  private Integer employeeId;

  @NaturalId
  @Column
  private String email;

  @Column
  private String firstName;

  @Column
  private String lastName;

  public Integer getEmployeeId() {
    return employeeId;
  }

  public void setEmployeeId(Integer employeeId) {
    this.employeeId = employeeId;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }
}
