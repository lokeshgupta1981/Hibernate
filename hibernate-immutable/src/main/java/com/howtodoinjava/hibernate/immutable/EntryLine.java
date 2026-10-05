package com.howtodoinjava.hibernate.immutable;

import java.math.BigDecimal;

// A Java record used as a read-only query result (not an entity)
public record EntryLine(String description, BigDecimal amount) {
}
