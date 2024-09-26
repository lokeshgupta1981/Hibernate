package com.howtodoinjava.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

@Data
@AllArgsConstructor
@NoArgsConstructor

@Entity
@Table
@Indexed
public class Book {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @DocumentId
  private Long id;

  @Column(nullable = false)
  @FullTextField
  @KeywordField(name = "sort_title", sortable = Sortable.YES)
  private String title;

  @Column(nullable = false)
  @FullTextField
  @KeywordField(name = "sort_author", sortable = Sortable.YES)
  private String author;
}
