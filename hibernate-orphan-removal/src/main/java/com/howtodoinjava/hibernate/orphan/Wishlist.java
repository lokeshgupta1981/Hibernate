package com.howtodoinjava.hibernate.orphan;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

// Same shape as Cart, but WITHOUT orphanRemoval
@Entity
public class Wishlist {

  @Id
  @GeneratedValue
  private Long id;

  private String owner;

  @OneToMany(mappedBy = "wishlist", cascade = CascadeType.ALL)
  private List<WishlistItem> items = new ArrayList<>();

  protected Wishlist() {
  }

  public Wishlist(String owner) {
    this.owner = owner;
  }

  public void addItem(WishlistItem item) {
    items.add(item);
    item.setWishlist(this);
  }

  public void removeItem(WishlistItem item) {
    items.remove(item);
    item.setWishlist(null);
  }

  public WishlistItem findItem(String name) {
    return items.stream().filter(i -> i.getName().equals(name)).findFirst().orElseThrow();
  }

  public Long getId() {
    return id;
  }

  public List<WishlistItem> getItems() {
    return items;
  }
}
