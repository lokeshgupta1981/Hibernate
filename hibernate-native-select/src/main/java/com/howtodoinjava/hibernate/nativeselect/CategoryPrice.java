package com.howtodoinjava.hibernate.nativeselect;

import java.math.BigDecimal;

public record CategoryPrice(String category, Long items, BigDecimal cheapest) {
}
