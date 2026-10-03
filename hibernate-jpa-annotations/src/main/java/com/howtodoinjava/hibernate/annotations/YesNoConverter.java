package com.howtodoinjava.hibernate.annotations;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class YesNoConverter implements AttributeConverter<Boolean, String> {

  @Override
  public String convertToDatabaseColumn(Boolean value) {
    return value != null && value ? "Y" : "N";
  }

  @Override
  public Boolean convertToEntityAttribute(String column) {
    return "Y".equals(column);
  }
}
