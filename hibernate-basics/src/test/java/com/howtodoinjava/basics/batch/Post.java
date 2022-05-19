package com.howtodoinjava.basics.batch;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Entity
@Builder
@AllArgsConstructor
@Getter
public class Post {
  public Post() {
  }

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  private String title;
  private String content;

  @OneToMany(mappedBy = "post")
  private List<Comment> comments;

  public void setTitle(String title) {
    this.title = title;
  }
}
