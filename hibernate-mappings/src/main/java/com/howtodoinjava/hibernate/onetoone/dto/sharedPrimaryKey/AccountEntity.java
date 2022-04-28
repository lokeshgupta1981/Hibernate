package com.howtodoinjava.hibernate.onetoone.dto.sharedPrimaryKey;

import jakarta.persistence.*;

import java.io.Serializable;

@Entity(name = "SharedPrimaryKeyAccountEntity")
@Table(name = "ACCOUNT", uniqueConstraints = {
  @UniqueConstraint(columnNames = "ID")})
public class AccountEntity implements Serializable {

  private static final long serialVersionUID = -6790693372846798580L;

  @Id
  @Column(name = "ID", unique = true, nullable = false)
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Integer accountId;

  @Column(name = "ACC_NUMBER", unique = true, nullable = false, length = 100)
  private String accountNumber;

  @OneToOne(mappedBy = "account", cascade = CascadeType.ALL)
  private EmployeeEntity employee;

  public EmployeeEntity getEmployee() {
    return employee;
  }

  public void setEmployee(EmployeeEntity employee) {
    this.employee = employee;
  }

  public Integer getAccountId() {
    return accountId;
  }

  public void setAccountId(Integer accountId) {
    this.accountId = accountId;
  }

  public String getAccountNumber() {
    return accountNumber;
  }

  public void setAccountNumber(String accountNumber) {
    this.accountNumber = accountNumber;
  }
}
