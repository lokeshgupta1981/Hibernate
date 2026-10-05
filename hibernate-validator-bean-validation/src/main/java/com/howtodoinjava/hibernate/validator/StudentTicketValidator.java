package com.howtodoinjava.hibernate.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StudentTicketValidator implements ConstraintValidator<StudentTicket, Attendee> {

  private int maxAge;

  @Override
  public void initialize(StudentTicket annotation) {
    maxAge = annotation.maxAge();
  }

  @Override
  public boolean isValid(Attendee attendee, ConstraintValidatorContext context) {
    if (attendee == null || attendee.getTicketType() != TicketType.STUDENT) {
      return true;
    }
    if (attendee.getAge() <= maxAge) {
      return true;
    }
    context.disableDefaultConstraintViolation();
    context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
        .addPropertyNode("age")
        .addConstraintViolation();
    return false;
  }
}
