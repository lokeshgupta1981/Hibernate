package com.howtodoinjava.hibernate.lazy;

import java.math.BigDecimal;

/** DTO projection: only the columns a menu page shows. */
public record MenuLine(String restaurant, String item, BigDecimal price) {
}
