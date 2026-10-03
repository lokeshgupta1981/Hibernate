package com.howtodoinjava.hibernate.immutable;

// A mutable value class, stored in one column through MemoConverter
public class Memo {

  private String text;

  public Memo(String text) {
    this.text = text;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }
}
