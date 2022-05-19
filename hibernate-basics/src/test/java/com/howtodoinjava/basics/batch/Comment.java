package com.howtodoinjava.basics.batch;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Builder
@AllArgsConstructor
public class Comment {

  public Comment() {
  }

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;
  private String text;
  @ManyToOne
  private Post post;
}
