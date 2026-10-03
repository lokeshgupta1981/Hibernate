package com.howtodoinjava.hibernate.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;

@Entity
public class Runner {

  @Id
  @GeneratedValue
  private Long id;

  private String name;
  private String ageGroup;
  private String city;

  private Integer finishMinutes;     // null = did not finish
  private BigDecimal entryFee;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "club_id")
  private Club club;

  protected Runner() {
  }

  public Runner(String name, String ageGroup, String city, Integer finishMinutes,
      BigDecimal entryFee, Club club) {
    this.name = name;
    this.ageGroup = ageGroup;
    this.city = city;
    this.finishMinutes = finishMinutes;
    this.entryFee = entryFee;
    this.club = club;
    if (club != null) {
      club.getMembers().add(this);
    }
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getAgeGroup() {
    return ageGroup;
  }

  public String getCity() {
    return city;
  }

  public Integer getFinishMinutes() {
    return finishMinutes;
  }

  public BigDecimal getEntryFee() {
    return entryFee;
  }

  public Club getClub() {
    return club;
  }
}
