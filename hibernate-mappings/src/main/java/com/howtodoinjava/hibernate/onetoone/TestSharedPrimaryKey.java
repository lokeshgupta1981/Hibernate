package com.howtodoinjava.hibernate.onetoone;

import com.howtodoinjava.hibernate.onetoone.dto.sharedPrimaryKey.AccountEntity;
import com.howtodoinjava.hibernate.onetoone.dto.sharedPrimaryKey.EmployeeEntity;

import org.hibernate.Session;

public class TestSharedPrimaryKey
{
	
	public static void main(String[] args) 
	{
		Session session = HibernateUtil.getSessionFactory().openSession();
		session.beginTransaction();
       
		AccountEntity account = new AccountEntity();
		account.setAccountNumber("123-345-65454");
		
		//Add new Employee object
		EmployeeEntity emp = new EmployeeEntity();
		emp.setEmail("demo-user@mail.com");
		emp.setFirstName("demo");
		emp.setLastName("user");
		
		emp.setAccount(account);
		account.setEmployee(emp);
		//Save Employee
		session.save(emp);
		
		
		session.getTransaction().commit();
		HibernateUtil.shutdown();
	}

}
