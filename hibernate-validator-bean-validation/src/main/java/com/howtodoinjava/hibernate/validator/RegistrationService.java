package com.howtodoinjava.hibernate.validator;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RegistrationService {

  @NotNull
  @Valid
  public Attendee register(@NotBlank String name, @NotNull @Email String email, @Min(18) int age) {
    return new Attendee(name, email, age, TicketType.STANDARD);
  }

  @NotNull
  public Attendee findByEmail(String email) {
    return null;    // nobody registered with this email
  }
}
