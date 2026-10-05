package com.howtodoinjava.hibernate.searchboot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

@Entity
@Indexed
public class Wine {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @FullTextField(analyzer = "wine")
  @KeywordField(name = "name_sort", sortable = Sortable.YES, normalizer = "lowercase")
  private String name;

  @KeywordField(normalizer = "lowercase")
  private String region;

  @FullTextField(analyzer = "wine")
  private String grape;

  @FullTextField(analyzer = "wine")
  @Column(length = 1000)
  private String tastingNotes;

  @GenericField(sortable = Sortable.YES)
  @Column(precision = 8, scale = 2)
  private BigDecimal price;

  protected Wine() {
  }

  public Wine(String name, String region, String grape, String tastingNotes, BigDecimal price) {
    this.name = name;
    this.region = region;
    this.grape = grape;
    this.tastingNotes = tastingNotes;
    this.price = price;
  }

  public Long getId() { return id; }
  public String getName() { return name; }
  public String getRegion() { return region; }
  public String getGrape() { return grape; }
  public String getTastingNotes() { return tastingNotes; }
  public BigDecimal getPrice() { return price; }

  public void setName(String name) { this.name = name; }
  public void setRegion(String region) { this.region = region; }
  public void setGrape(String grape) { this.grape = grape; }
  public void setTastingNotes(String tastingNotes) { this.tastingNotes = tastingNotes; }
  public void setPrice(BigDecimal price) { this.price = price; }

  @Override
  public String toString() {
    return name;
  }
}
