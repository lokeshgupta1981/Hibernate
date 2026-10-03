package com.howtodoinjava.hibernate.nativedelete;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PreRemove;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Entity
@Table(name = "coupon")
@NamedNativeQuery(
    name = "Coupon.deleteExpired",
    query = "delete from coupon where expires_on < :today")
@NamedNativeQuery(
    name = "Coupon.deleteByDiscount",
    query = "delete from coupon where discount_percent >= ?1")
public class Coupon {

  /** Codes of the coupons for which the @PreRemove callback ran. */
  public static final List<String> REMOVE_CALLBACKS = new CopyOnWriteArrayList<>();

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String code;

  @Column(name = "discount_percent")
  private int discountPercent;

  @Column(name = "expires_on")
  private LocalDate expiresOn;

  @OneToMany(mappedBy = "coupon", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Redemption> redemptions = new ArrayList<>();

  protected Coupon() {
  }

  public Coupon(String code, int discountPercent, LocalDate expiresOn) {
    this.code = code;
    this.discountPercent = discountPercent;
    this.expiresOn = expiresOn;
  }

  public void redeem(LocalDateTime redeemedAt) {
    Redemption redemption = new Redemption(this, redeemedAt);
    redemptions.add(redemption);
  }

  @PreRemove
  void beforeRemove() {
    REMOVE_CALLBACKS.add(code);
    System.out.println("@PreRemove called for " + code);
  }

  public Long getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public int getDiscountPercent() {
    return discountPercent;
  }

  public LocalDate getExpiresOn() {
    return expiresOn;
  }

  public void setExpiresOn(LocalDate expiresOn) {
    this.expiresOn = expiresOn;
  }

  public List<Redemption> getRedemptions() {
    return redemptions;
  }

  @Override
  public String toString() {
    return "Coupon[" + code + ", " + discountPercent + "%, expires " + expiresOn + "]";
  }
}
