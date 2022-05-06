package com.howtodoinjava.demo.entity;


import jakarta.persistence.*;

import java.io.Serial;
import java.io.Serializable;

@NamedQueries({
    @NamedQuery(name = "QUERY_GET_DEPARTMENT_BY_ID",
        query = "from DepartmentEntity d WHERE d.id = :id"),
    @NamedQuery(name = "QUERY_UPDATE_DEPARTMENT_BY_ID",
        query = "UPDATE DepartmentEntity d SET d.name=:name WHERE d.id = :id")
})
@NamedNativeQueries({
    @NamedNativeQuery(name = "NATIVE_QUERY_GET_DEPARTMENT_BY_ID",
        query = "SELECT * FROM TBL_DEPT d WHERE d.id = :id"),
    @NamedNativeQuery(name = "NATIVE_QUERY_UPDATE_DEPARTMENT_BY_ID",
        query = "UPDATE TBL_DEPT d SET d.name=:name WHERE d.id = :id")
})
@Entity
@Table(name = "TBL_DEPT")
public class DepartmentEntity implements Serializable {

  @Serial
  private static final long serialVersionUID = -1;

  public static final String QUERY_GET_DEPARTMENT_BY_ID
      = "QUERY_GET_DEPARTMENT_BY_ID";

  public static final String QUERY_UPDATE_DEPARTMENT_BY_ID
      = "QUERY_UPDATE_DEPARTMENT_BY_ID";

  public static final String NATIVE_QUERY_GET_DEPARTMENT_BY_ID
      = "NATIVE_QUERY_GET_DEPARTMENT_BY_ID";

  public static final String NATIVE_QUERY_UPDATE_DEPARTMENT_BY_ID
      = "NATIVE_QUERY_UPDATE_DEPARTMENT_BY_ID";

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Integer id;

  private String name;

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
}
