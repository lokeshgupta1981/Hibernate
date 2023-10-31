package com.howtodoinjava.hibernate.demo.model;

import jakarta.persistence.*;

import java.io.Serializable;

@Entity
@Table(name = "tbl_employee")
@SqlResultSetMapping(name = "updateResult", columns = {@ColumnResult(name = "count")})
@SqlResultSetMapping(name = "deleteResult", columns = {@ColumnResult(name = "count")})
@NamedNativeQueries({
  @NamedNativeQuery(
    name = "getEmployeesByDeptId",
    query = "SELECT e.id, e.firstName, e.lastName, e.email, d.id as department_id, d.name " +
      "FROM tbl_employee e " +
      "INNER JOIN tbl_department d ON d.id = e.department_id " +
      "WHERE d.id = :deptId",
    resultClass = Employee.class
  ),
  @NamedNativeQuery(
    name = "updateEmployeeName",
    query = "UPDATE tbl_employee SET firstName = :newFirstName, lastName = :newLastName WHERE id = :employeeId",
    resultSetMapping = "updateResult"
  ),
  @NamedNativeQuery(
    name = "deleteEmployeeById",
    query = "DELETE FROM tbl_employee WHERE id = :employeeId"
    , resultSetMapping = "deleteResult"
  )
})
public class Employee implements Serializable {

  @Id
  @GeneratedValue
  private Integer id;
  private String firstName;
  private String lastName;
  private String email;

  @ManyToOne
  private Department department;

  public Employee() {
  }

  public Employee(String firstName, String lastName, String email, Department department) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.department = department;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
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

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Department getDepartment() {
    return department;
  }

  public void setDepartment(Department department) {
    this.department = department;
  }

  @Override
  public String toString() {
    return "Employee{" +
      "id=" + id +
      ", firstName='" + firstName + '\'' +
      ", lastName='" + lastName + '\'' +
      ", email='" + email + '\'' +
      ", department=" + department +
      '}';
  }
}
