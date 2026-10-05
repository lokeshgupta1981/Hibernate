package com.howtodoinjava.hibernate.criteria;

import java.math.BigDecimal;

/** A DTO filled by cb.construct(): only the title and the price of a listing. */
public record ListingSummary(String title, BigDecimal price) {
}
