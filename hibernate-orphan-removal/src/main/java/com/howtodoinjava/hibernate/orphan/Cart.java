package com.howtodoinjava.hibernate.orphan;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Cart {

  @Id
  @GeneratedValue
  private Long id;

  private String owner;

  @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Item> items = new ArrayList<>();

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "coupon_id")
  private Coupon coupon;

  protected Cart() {
  }

  public Cart(String owner) {
    this.owner = owner;
  }

  public void addItem(Item item) {
    items.add(item);
    item.setCart(this);
  }

  public void removeItem(Item item) {
    items.remove(item);
    item.setCart(null);
  }

  public Item findItem(String name) {
    return items.stream().filter(i -> i.getName().equals(name)).findFirst().orElseThrow();
  }

  public Long getId() {
    return id;
  }

  public String getOwner() {
    return owner;
  }

  public List<Item> getItems() {
    return items;
  }

  // Used only to show the "collection was no longer referenced" error
  public void setItems(List<Item> items) {
    this.items = items;
  }

  public Coupon getCoupon() {
    return coupon;
  }

  public void setCoupon(Coupon coupon) {
    this.coupon = coupon;
  }
}
