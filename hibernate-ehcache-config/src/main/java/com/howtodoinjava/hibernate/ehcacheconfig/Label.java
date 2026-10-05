package com.howtodoinjava.hibernate.ehcacheconfig;

/** Common view of the four label entities, one per cache concurrency strategy. */
public interface Label {

  Long getId();

  String getText();

  void setText(String text);
}
