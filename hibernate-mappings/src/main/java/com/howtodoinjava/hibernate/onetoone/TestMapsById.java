package com.howtodoinjava.hibernate.onetoone;

import com.howtodoinjava.hibernate.onetoone.dto.mapsById.AccountEntity;
import com.howtodoinjava.hibernate.onetoone.dto.mapsById.EmployeeEntity;

import org.hibernate.Session;

public class TestMapsById
{
	
	public static void main(String[] args) 
	{
		Session session = HibernateUtil.getSessionFactory().openSession();
		session.beginTransaction();
       
		AccountEntity account = new AccountEntity();
		account.setAccountNumber("123-345-65454");
		//session.save(account);
		
		//Add new Employee object
		EmployeeEntity emp = new EmployeeEntity();
		emp.setEmail("demo-user@mail.com");
		emp.setFirstName("demo");
		emp.setLastName("user");
		
		emp.setAccount(account);
		session.save(emp);
		
		session.getTransaction().commit();
		HibernateUtil.shutdown();
	}

}
