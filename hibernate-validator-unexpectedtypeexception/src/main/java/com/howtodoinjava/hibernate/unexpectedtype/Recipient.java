package com.howtodoinjava.hibernate.unexpectedtype;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Our own type: the person who receives the gift card.
 * The constraints sit on the String components, where the built-in validators work.
 */
public record Recipient(@NotBlank String name, @NotBlank @Email String email) {
}
