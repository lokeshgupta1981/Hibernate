package com.howtodoinjava.hibernate.nativedelete;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "redemption")
public class Redemption {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "coupon_id", foreignKey = @ForeignKey(name = "fk_redemption_coupon"))
  private Coupon coupon;

  @Column(name = "redeemed_at")
  private LocalDateTime redeemedAt;

  protected Redemption() {
  }

  Redemption(Coupon coupon, LocalDateTime redeemedAt) {
    this.coupon = coupon;
    this.redeemedAt = redeemedAt;
  }

  public Long getId() {
    return id;
  }

  public Coupon getCoupon() {
    return coupon;
  }

  public LocalDateTime getRedeemedAt() {
    return redeemedAt;
  }
}
