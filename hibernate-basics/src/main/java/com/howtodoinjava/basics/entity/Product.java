package com.howtodoinjava.basics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@Builder
@Entity
@Table(name = "TBL_PRODUCT")
@Indexed
public class Product {
  @Id
  @Column(name = "id", nullable = false)
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  @KeywordField
  private String name;

  @KeywordField
  private String company;

  @FullTextField
  private String features;
  
  @GenericField
  private LocalDate launchedOn;

  public Product() {
  }
}
