package com.howtodoinjava.hibernate.oneToMany;

import com.howtodoinjava.hibernate.oneToMany.joinTable.AccountEntity;
import com.howtodoinjava.hibernate.oneToMany.joinTable.EmployeeEntity;
import org.hibernate.Session;

import java.util.HashSet;
import java.util.Set;

public class TestJoinTable {

  public static void main(String[] args) {
    Session session = HibernateUtil.getSessionFactory().openSession();
    session.beginTransaction();

    AccountEntity account1 = new AccountEntity();
    account1.setAccountNumber("123-345-65454");

    AccountEntity account2 = new AccountEntity();
    account2.setAccountNumber("123-345-6542222");

    //Add new Employee object
    EmployeeEntity emp = new EmployeeEntity();
    emp.setEmail("demo-user@mail.com");
    emp.setFirstName("demo");
    emp.setLastName("user");

    Set<AccountEntity> accounts = new HashSet<>();
    accounts.add(account1);
    accounts.add(account2);

    emp.setAccounts(accounts);
    //Save Employee
    session.save(emp);

    session.getTransaction().commit();
    HibernateUtil.shutdown();
  }

}
