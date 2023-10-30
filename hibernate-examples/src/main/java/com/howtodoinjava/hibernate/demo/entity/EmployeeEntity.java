package com.howtodoinjava.hibernate.demo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.SoftDelete;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

@Entity
@Table(name = "Employee", uniqueConstraints = {
  @UniqueConstraint(columnNames = "ID"),
  @UniqueConstraint(columnNames = "EMAIL")})
@NamedNativeQueries({
  @NamedNativeQuery(
    name = "getInactiveEmployees",
    query = "SELECT id, firstName, lastName, email FROM Employee e where e.deleted = 1",
    resultClass=EmployeeEntity.class
  )
})
@SoftDelete
public class EmployeeEntity implements Serializable {

  public EmployeeEntity(){
  }

  public EmployeeEntity(String firstName, String lastName, String email){
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "ID")
  private Integer employeeId;

  @NaturalId
  @Column
  private String email;

  @Column
  private String firstName;

  @Column
  private String lastName;

  @ElementCollection
  @CollectionTable(name = "Emails", joinColumns = @JoinColumn(name = "employeeId"))
  @Column(name = "Email")
  @SoftDelete
  private Collection<String> phoneNumbers = new ArrayList<>();

  public Collection<String> getPhoneNumbers() {
    return phoneNumbers;
  }

  public void setPhoneNumbers(Collection<String> phoneNumbers) {
    this.phoneNumbers.addAll(phoneNumbers);
  }

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

  @Override
  public String toString() {
    return "EmployeeEntity{" +
      "employeeId=" + employeeId +
      ", email='" + email + '\'' +
      ", firstName='" + firstName + '\'' +
      ", lastName='" + lastName + '\'' +
      ", phoneNumbers=" + phoneNumbers +
      '}';
  }
}
