package com.howtodoinjava.hibernate.manyToMany.joinTable;

import java.io.Serializable;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity(name = "ReaderEntity")
@Table(name = "READER", uniqueConstraints = {
		@UniqueConstraint(columnNames = "ID"),
		@UniqueConstraint(columnNames = "EMAIL") })
public class ReaderEntity implements Serializable {

	private static final long serialVersionUID = -1798070786993154676L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID", unique = true, nullable = false)
	private Integer readerId;

	@Column(name = "EMAIL", unique = true, nullable = false, length = 100)
	private String email;

	@Column(name = "FIRST_NAME", unique = false, nullable = false, length = 100)
	private String firstName;

	@Column(name = "LAST_NAME", unique = false, nullable = false, length = 100)
	private String lastName;

	@ManyToMany(cascade=CascadeType.ALL)
	@JoinTable(name="READER_SUBSCRIPTIONS", joinColumns={@JoinColumn(referencedColumnName="ID")}
										, inverseJoinColumns={@JoinColumn(referencedColumnName="ID")})	
	private Set<SubscriptionEntity> subscriptions;

	public Integer getReaderId() {
		return readerId;
	}

	public void setReaderId(Integer readerId) {
		this.readerId = readerId;
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

	public Set<SubscriptionEntity> getSubscriptions() {
		return subscriptions;
	}

	public void setSubscriptions(Set<SubscriptionEntity> subscriptions) {
		this.subscriptions = subscriptions;
	}
}
