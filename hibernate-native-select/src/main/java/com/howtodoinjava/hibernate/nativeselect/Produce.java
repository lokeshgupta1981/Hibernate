package com.howtodoinjava.hibernate.nativeselect;

import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FetchType;
import jakarta.persistence.FieldResult;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.SqlResultSetMapping;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@NamedNativeQuery(name = "Produce.findByCategory",
    query = "select * from Produce where category = :category order by name",
    resultClass = Produce.class)
@NamedNativeQuery(name = "Produce.categoryPrices",
    query = "select category, count(*) as items, min(pricePerKg) as cheapest "
        + "from Produce group by category order by category",
    resultSetMapping = "CategoryPriceMapping")
@NamedNativeQuery(name = "Produce.withFarmByRegion",
    query = "select p.*, f.name as farm_name, f.region "
        + "from Produce p join Farm f on f.id = p.farm_id "
        + "where f.region = :region order by p.name",
    resultSetMapping = "ProduceWithFarmMapping")
// Jakarta Persistence 3.2 allows the mapping inside @NamedNativeQuery, but Hibernate 7.4.11
// ignores the entities attribute: each row comes back as plain column values.
@NamedNativeQuery(name = "Produce.withFarmInline",
    query = "select p.*, f.name as farm_name, f.region "
        + "from Produce p join Farm f on f.id = p.farm_id "
        + "where f.region = :region order by p.id",
    entities = {
        @EntityResult(entityClass = Produce.class),
        @EntityResult(entityClass = Farm.class, fields = {
            @FieldResult(name = "id", column = "farm_id"),
            @FieldResult(name = "name", column = "farm_name"),
            @FieldResult(name = "region", column = "region")})})
// Hibernate 7.4.11 cannot run this one: a record as resultClass of a NAMED native query
// fails with JdbcTypeRecommendationException. Use a @SqlResultSetMapping instead.
@NamedNativeQuery(name = "Produce.pricesAsRecord",
    query = "select name, pricePerKg from Produce order by pricePerKg",
    resultClass = ProducePrice.class)
@SqlResultSetMapping(name = "CategoryPriceMapping",
    classes = @ConstructorResult(targetClass = CategoryPrice.class,
        columns = {
            @ColumnResult(name = "category"),
            @ColumnResult(name = "items", type = Long.class),
            @ColumnResult(name = "cheapest", type = BigDecimal.class)}))
@SqlResultSetMapping(name = "ProduceWithFarmMapping",
    entities = {
        @EntityResult(entityClass = Produce.class),
        @EntityResult(entityClass = Farm.class, fields = {
            @FieldResult(name = "id", column = "farm_id"),
            @FieldResult(name = "name", column = "farm_name"),
            @FieldResult(name = "region", column = "region")})})
public class Produce {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;
  private String category;

  @Column(precision = 6, scale = 2)
  private BigDecimal pricePerKg;

  private LocalDate harvestedOn;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "farm_id")
  private Farm farm;

  protected Produce() {
  }

  public Produce(String name, String category, String pricePerKg, LocalDate harvestedOn, Farm farm) {
    this.name = name;
    this.category = category;
    this.pricePerKg = new BigDecimal(pricePerKg);
    this.harvestedOn = harvestedOn;
    this.farm = farm;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getCategory() {
    return category;
  }

  public BigDecimal getPricePerKg() {
    return pricePerKg;
  }

  public void setPricePerKg(BigDecimal pricePerKg) {
    this.pricePerKg = pricePerKg;
  }

  public LocalDate getHarvestedOn() {
    return harvestedOn;
  }

  public Farm getFarm() {
    return farm;
  }

  @Override
  public String toString() {
    return name + " " + pricePerKg;
  }
}
