package com.howtodoinjava.hibernate.immutable;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.hibernate.annotations.Immutable;

@Converter
@Immutable
public class MemoConverter implements AttributeConverter<Memo, String> {

  @Override
  public String convertToDatabaseColumn(Memo memo) {
    return memo == null ? null : memo.getText();
  }

  @Override
  public Memo convertToEntityAttribute(String text) {
    return text == null ? null : new Memo(text);
  }
}
