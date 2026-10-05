package com.howtodoinjava.hibernate.datetime;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDate;
import java.time.YearMonth;

/** Stores a YearMonth as the first day of the month in a date column. */
@Converter
public class YearMonthConverter implements AttributeConverter<YearMonth, LocalDate> {

  @Override
  public LocalDate convertToDatabaseColumn(YearMonth month) {
    return month == null ? null : month.atDay(1);
  }

  @Override
  public YearMonth convertToEntityAttribute(LocalDate date) {
    return date == null ? null : YearMonth.from(date);
  }
}
