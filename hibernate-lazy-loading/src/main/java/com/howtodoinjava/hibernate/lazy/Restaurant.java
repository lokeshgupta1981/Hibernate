package com.howtodoinjava.hibernate.lazy;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Entity
@NamedEntityGraph(name = "Restaurant.menu", attributeNodes = @NamedAttributeNode("menu"))
public class Restaurant {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL)   // LAZY by default
  private List<MenuItem> menu = new ArrayList<>();

  protected Restaurant() {
  }

  public Restaurant(String name) {
    this.name = name;
  }

  public void addItem(MenuItem item) {
    menu.add(item);
    item.setRestaurant(this);
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public List<MenuItem> getMenu() {
    return menu;
  }

  @Override
  public String toString() {
    return name;
  }
}
