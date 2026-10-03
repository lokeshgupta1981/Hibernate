package com.howtodoinjava.hibernate.immutable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.Immutable;

@Entity
public class Journal {

  @Id
  @GeneratedValue
  private Long id;

  private String name;

  @Immutable
  private LocalDate openedOn;

  @Column(updatable = false)
  private String owner;

  @Convert(converter = MemoConverter.class)
  private Memo memo;

  @OneToMany(mappedBy = "journal", cascade = CascadeType.PERSIST)
  @Immutable
  private List<LedgerEntry> entries = new ArrayList<>();

  protected Journal() {
  }

  public Journal(String name, String owner, LocalDate openedOn) {
    this.name = name;
    this.owner = owner;
    this.openedOn = openedOn;
  }

  public void post(LedgerEntry entry) {
    entries.add(entry);
    entry.setJournal(this);
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public LocalDate getOpenedOn() {
    return openedOn;
  }

  public void setOpenedOn(LocalDate openedOn) {
    this.openedOn = openedOn;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public Memo getMemo() {
    return memo;
  }

  public void setMemo(Memo memo) {
    this.memo = memo;
  }

  public List<LedgerEntry> getEntries() {
    return entries;
  }
}
