package com.howtodoinjava.hibernate.orphan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.RollbackException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrphanRemovalTest {

  private EntityManagerFactory emf;
  private Long cartId;

  @BeforeEach
  void setUp() {
    emf = Database.create(false);
    cartId = emf.callInTransaction(em -> {
      Tag fruit = new Tag("fruit");
      em.persist(fruit);
      Cart cart = new Cart("Lokesh");
      Item apple = new Item("apple");
      Item banana = new Item("banana");
      Item milk = new Item("milk");
      banana.addTag(fruit);
      cart.addItem(apple);
      cart.addItem(banana);
      cart.addItem(milk);
      cart.setCoupon(new Coupon(10));
      em.persist(cart);
      return cart.getId();
    });
  }

  @AfterEach
  void tearDown() {
    emf.close();
  }

  private long count(String entity) {
    return Database.count(emf, entity);
  }

  @Test
  void removingItemFromCollectionDeletesTheRow() {
    emf.runInTransaction(em -> {
      Cart cart = em.find(Cart.class, cartId);
      cart.removeItem(cart.findItem("apple"));
    });
    assertEquals(2, count("Item"));
    long apples = emf.callInTransaction(em -> em
        .createQuery("select count(*) from Item where name = 'apple'", Long.class)
        .getSingleResult());
    assertEquals(0, apples);
  }

  @Test
  void withoutOrphanRemovalTheRowStaysWithNullForeignKey() {
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
    assertEquals(2, count("WishlistItem"));
    long orphans = emf.callInTransaction(em -> em
        .createQuery("select count(*) from WishlistItem where wishlist is null", Long.class)
        .getSingleResult());
    assertEquals(1, orphans);
  }

  @Test
  void settingOneToOneToNullDeletesTheCoupon() {
    emf.runInTransaction(em -> em.find(Cart.class, cartId).setCoupon(null));
    assertEquals(0, count("Coupon"));
  }

  @Test
  void replacingTheCouponDeletesTheOldOne() {
    emf.runInTransaction(em -> em.find(Cart.class, cartId).setCoupon(new Coupon(20)));
    assertEquals(1, count("Coupon"));
    int discount = emf.callInTransaction(em -> em
        .createQuery("select c.discount from Coupon c", Integer.class).getSingleResult());
    assertEquals(20, discount);
  }

  @Test
  void mergingDetachedCartAppliesOrphanRemoval() {
    Cart detached = emf.callInTransaction(em -> em
        .createQuery("select c from Cart c join fetch c.items where c.id = :id", Cart.class)
        .setParameter("id", cartId).getSingleResult());
    detached.removeItem(detached.findItem("apple"));
    emf.runInTransaction(em -> em.merge(detached));
    assertEquals(2, count("Item"));
  }

  @Test
  void replacingTheCollectionFails() {
    RollbackException e = assertThrows(RollbackException.class, () ->
        emf.runInTransaction(em -> em.find(Cart.class, cartId).setItems(new ArrayList<>())));
    assertTrue(e.getMessage().contains(
        "A collection with orphan deletion was no longer referenced by the owning entity instance"));
    assertEquals(3, count("Item"));
  }

  @Test
  void clearingTheCollectionDeletesAllItems() {
    emf.runInTransaction(em -> {
      Cart cart = em.find(Cart.class, cartId);
      cart.getItems().clear();
    });
    assertEquals(0, count("Item"));
    assertEquals(1, count("Cart"));
  }

  @Test
  void swappingItemsInsideTheManagedCollectionWorks() {
    emf.runInTransaction(em -> {
      Cart cart = em.find(Cart.class, cartId);
      List<Item> newItems = List.of(new Item("bread"), new Item("eggs"));
      cart.getItems().clear();
      newItems.forEach(cart::addItem);
    });
    List<String> names = emf.callInTransaction(em -> em
        .createQuery("select name from Item order by name", String.class).getResultList());
    assertEquals(List.of("bread", "eggs"), names);
  }

  @Test
  void removingManyToManyLinkKeepsTheTag() {
    emf.runInTransaction(em -> {
      Item banana = em.find(Cart.class, cartId).findItem("banana");
      banana.removeTag(banana.getTags().iterator().next());
    });
    assertEquals(1, count("Tag"));
    long links = emf.callInTransaction(em -> ((Number) em
        .createNativeQuery("select count(*) from item_tag").getSingleResult()).longValue());
    assertEquals(0, links);
  }

  @Test
  void movingItemToAnotherCartUpdatesTheForeignKeyInHibernate() {
    Long otherId = emf.callInTransaction(em -> {
      Cart other = new Cart("Alex");
      em.persist(other);
      return other.getId();
    });
    emf.runInTransaction(em -> {
      Cart cart = em.find(Cart.class, cartId);
      Cart other = em.find(Cart.class, otherId);
      Item milk = cart.findItem("milk");
      cart.removeItem(milk);
      other.addItem(milk);
    });
    assertEquals(3, count("Item"));
    long inOther = emf.callInTransaction(em -> em
        .createQuery("select count(*) from Item where cart.id = :id", Long.class)
        .setParameter("id", otherId).getSingleResult());
    assertEquals(1, inOther);
  }

  @Test
  void deletingTheCartDeletesItemsAndCoupon() {
    emf.runInTransaction(em -> em.remove(em.find(Cart.class, cartId)));
    assertEquals(0, count("Cart"));
    assertEquals(0, count("Item"));
    assertEquals(0, count("Coupon"));
    assertEquals(1, count("Tag"));
  }

  @Test
  void bulkDeleteIgnoresOrphanRemovalAndCascade() {
    assertThrows(PersistenceException.class, () ->
        emf.runInTransaction(em -> em.createQuery("delete from Cart").executeUpdate()));
    assertEquals(3, count("Item"));
  }
}
