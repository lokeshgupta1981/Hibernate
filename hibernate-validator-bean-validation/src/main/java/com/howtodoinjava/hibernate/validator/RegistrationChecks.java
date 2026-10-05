package com.howtodoinjava.hibernate.validator;

import jakarta.validation.GroupSequence;
import jakarta.validation.groups.Default;

/** Checks the Default group first; the Checkout group runs only when Default passes. */
@GroupSequence({Default.class, Checkout.class})
public interface RegistrationChecks {
}
