package com.howtodoinjava.hibernate.validator;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** Same text checked by @NotNull, @NotEmpty and @NotBlank to compare the three constraints. */
public record BadgeText(
    @NotNull String notNull,
    @NotEmpty String notEmpty,
    @NotBlank String notBlank) {
}
