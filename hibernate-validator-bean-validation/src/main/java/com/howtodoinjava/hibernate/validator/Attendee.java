package com.howtodoinjava.hibernate.validator;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

@Entity
@StudentTicket
public class Attendee {

  @Id
  @GeneratedValue
  private Long id;

  @NotBlank
  @Size(min = 2, max = 40, message = "{attendee.name.size}")
  private String name;

  @NotNull
  @Email
  private String email;

  @Min(18)
  @Max(120)
  private int age;

  @NotNull
  @Enumerated(EnumType.STRING)
  private TicketType ticketType;

  @Valid
  @Embedded
  private Address address;

  @ElementCollection
  private List<@Email String> guestEmails = new ArrayList<>();

  @AssertTrue(groups = Checkout.class, message = "terms must be accepted before payment")
  private boolean termsAccepted;

  protected Attendee() {
  }

  public Attendee(String name, String email, int age, TicketType ticketType) {
    this.name = name;
    this.email = email;
    this.age = age;
    this.ticketType = ticketType;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public int getAge() {
    return age;
  }

  public void setAge(int age) {
    this.age = age;
  }

  public TicketType getTicketType() {
    return ticketType;
  }

  public Address getAddress() {
    return address;
  }

  public void setAddress(Address address) {
    this.address = address;
  }

  public List<String> getGuestEmails() {
    return guestEmails;
  }

  public boolean isTermsAccepted() {
    return termsAccepted;
  }

  public void setTermsAccepted(boolean termsAccepted) {
    this.termsAccepted = termsAccepted;
  }
}
