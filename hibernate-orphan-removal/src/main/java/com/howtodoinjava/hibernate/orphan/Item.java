package com.howtodoinjava.hibernate.orphan;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Item {

  @Id
  @GeneratedValue
  private Long id;

  private String name;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cart_id")
  private Cart cart;

  @ManyToMany
  @JoinTable(name = "item_tag",
      joinColumns = @JoinColumn(name = "item_id"),
      inverseJoinColumns = @JoinColumn(name = "tag_id"))
  private Set<Tag> tags = new HashSet<>();

  protected Item() {
  }

  public Item(String name) {
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Cart getCart() {
    return cart;
  }

  void setCart(Cart cart) {
    this.cart = cart;
  }

  public Set<Tag> getTags() {
    return tags;
  }

  public void addTag(Tag tag) {
    tags.add(tag);
    tag.getItems().add(this);
  }

  public void removeTag(Tag tag) {
    tags.remove(tag);
    tag.getItems().remove(this);
  }
}
