package com.howtodoinjava.hibernate.validator;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** A conference talk. Constraints on record components apply to the fields and constructor parameters. */
public record Talk(
    @NotBlank String title,
    @NotBlank String speaker,
    @Min(15) @Max(60) int minutes) {
}
