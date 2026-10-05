package com.howtodoinjava.hibernate.validator;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = StudentTicketValidator.class)
@Target(TYPE)
@Retention(RUNTIME)
public @interface StudentTicket {

  String message() default "{attendee.student.age}";

  int maxAge() default 25;

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
