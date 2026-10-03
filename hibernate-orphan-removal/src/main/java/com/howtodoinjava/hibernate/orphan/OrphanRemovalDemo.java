package com.howtodoinjava.hibernate.orphan;

import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;

public class OrphanRemovalDemo {

  public static void main(String[] args) {
    try (EntityManagerFactory emf = Database.create(true)) {

      step("1. Save a cart with three items, two tags and a coupon");
      Long cartId = emf.callInTransaction(em -> {
        Tag fruit = new Tag("fruit");
        Tag dairy = new Tag("dairy");
        em.persist(fruit);
        em.persist(dairy);

        Cart cart = new Cart("Lokesh");
        Item apple = new Item("apple");
        Item banana = new Item("banana");
        Item milk = new Item("milk");
        banana.addTag(fruit);
        milk.addTag(dairy);
        cart.addItem(apple);
        cart.addItem(banana);
        cart.addItem(milk);
        cart.setCoupon(new Coupon(10));
        em.persist(cart);
        return cart.getId();
      });
      counts(emf);

      step("2. Remove 'apple' from the cart (orphanRemoval = true)");
      emf.runInTransaction(em -> {
        Cart cart = em.find(Cart.class, cartId);
        Item apple = cart.findItem("apple");
        cart.removeItem(apple);
      });
      counts(emf);

      step("3. Remove 'pen' from a wishlist (no orphanRemoval)");
      Long wishlistId = emf.callInTransaction(em -> {
        Wishlist wishlist = new Wishlist("Lokesh");
        wishlist.addItem(new WishlistItem("pen"));
        wishlist.addItem(new WishlistItem("book"));
        em.persist(wishlist);
        return wishlist.getId();
      });
      emf.runInTransaction(em -> {
        Wishlist wishlist = em.find(Wishlist.class, wishlistId);
        wishlist.removeItem(wishlist.findItem("pen"));
      });
      System.out.println("wishlist items in table: " + Database.count(emf, "WishlistItem"));

      step("4. Set the one-to-one coupon to null");
      emf.runInTransaction(em -> em.find(Cart.class, cartId).setCoupon(null));
      counts(emf);

      step("5. Replace the items collection with a new list");
      try {
        emf.runInTransaction(em -> em.find(Cart.class, cartId).setItems(new ArrayList<>()));
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }
      counts(emf);

      step("6. Remove the 'fruit' tag from 'banana' (many-to-many)");
      emf.runInTransaction(em -> {
        Item banana = em.find(Cart.class, cartId).findItem("banana");
        Tag fruit = banana.getTags().iterator().next();
        banana.removeTag(fruit);
      });
      counts(emf);

      step("7. Move 'milk' to another cart");
      Long otherCartId = emf.callInTransaction(em -> {
        Cart other = new Cart("Alex");
        em.persist(other);
        return other.getId();
      });
      emf.runInTransaction(em -> {
        Cart cart = em.find(Cart.class, cartId);
        Cart other = em.find(Cart.class, otherCartId);
        Item milk = cart.findItem("milk");
        cart.removeItem(milk);
        other.addItem(milk);
      });
      counts(emf);

      step("8. Delete the cart");
      emf.runInTransaction(em -> em.remove(em.find(Cart.class, cartId)));
      counts(emf);
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("== " + title + " ==");
  }

  private static void counts(EntityManagerFactory emf) {
    System.out.println("carts=" + Database.count(emf, "Cart")
        + " items=" + Database.count(emf, "Item")
        + " coupons=" + Database.count(emf, "Coupon")
        + " tags=" + Database.count(emf, "Tag"));
  }
}
