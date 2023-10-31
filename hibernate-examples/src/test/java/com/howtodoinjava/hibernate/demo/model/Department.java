package com.howtodoinjava.hibernate.demo.model;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tbl_department")
public class Department implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue
  private Integer id;
  private String name;

  @OneToMany(mappedBy = "department", cascade = CascadeType.PERSIST)
  private List<Employee> employees = new ArrayList<>();

  public Department() {
  }

  public Department(String name) {
    this.name = name;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<Employee> getEmployees() {
    return employees;
  }

  public void setEmployees(List<Employee> employees) {
    this.employees = employees;
  }

  @Override
  public String toString() {
    return "Department{" +
      "id=" + id +
      ", name='" + name + '\'' +
      ", employees=" + employees +
      '}';
  }
}
