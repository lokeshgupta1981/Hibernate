package com.howtodoinjava.hibernate.annotations;

import jakarta.persistence.EnumeratedValue;

public enum Language {
  ENGLISH("en"), HINDI("hi");

  @EnumeratedValue
  private final String code;

  Language(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
