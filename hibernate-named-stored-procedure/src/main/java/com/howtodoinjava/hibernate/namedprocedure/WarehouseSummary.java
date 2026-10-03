package com.howtodoinjava.hibernate.namedprocedure;

// DTO filled from the stock_summary result set through @ConstructorResult
public record WarehouseSummary(String warehouse, Long items, Long units) {
}
