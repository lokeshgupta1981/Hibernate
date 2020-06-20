package com.howtodoinjava.hibernate.oneToMany;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Session;

import com.howtodoinjava.hibernate.oneToMany.foreignKeyAsso.AccountEntity;
import com.howtodoinjava.hibernate.oneToMany.foreignKeyAsso.EmployeeEntity;

public class TestForeignKeyAssociation
{
	
	public static void main(String[] args) 
	{
		Session session = HibernateUtil.getSessionFactory().openSession();
		session.beginTransaction();
       
		AccountEntity account1 = new AccountEntity();
		account1.setAccountNumber("Account detail 1");
		
		AccountEntity account2 = new AccountEntity();
		account2.setAccountNumber("Account detail 2");
		
		AccountEntity account3 = new AccountEntity();
		account3.setAccountNumber("Account detail 3");
		
		//Add new Employee object
		EmployeeEntity firstEmployee = new EmployeeEntity();
		firstEmployee.setEmail("demo-user-first@mail.com");
		firstEmployee.setFirstName("demo-one");
		firstEmployee.setLastName("user-one");
		
		EmployeeEntity secondEmployee = new EmployeeEntity();
		secondEmployee.setEmail("demo-user-second@mail.com");
		secondEmployee.setFirstName("demo-two");
		secondEmployee.setLastName("user-two");
		
		Set<AccountEntity> accountsOfFirstEmployee = new HashSet<AccountEntity>();
		accountsOfFirstEmployee.add(account1);
		accountsOfFirstEmployee.add(account2);
		
		Set<AccountEntity> accountsOfSecondEmployee = new HashSet<AccountEntity>();
		accountsOfSecondEmployee.add(account3);
		
		firstEmployee.setAccounts(accountsOfFirstEmployee);
		secondEmployee.setAccounts(accountsOfSecondEmployee);
		//Save Employee
		session.save(firstEmployee);
		session.save(secondEmployee);
		
		session.getTransaction().commit();
		HibernateUtil.shutdown();
	}

}
