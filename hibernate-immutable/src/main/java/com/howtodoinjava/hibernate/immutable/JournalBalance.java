package com.howtodoinjava.hibernate.immutable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;

@Entity
@Immutable
@Subselect("""
    select j.id as journalId, j.name as name, sum(e.amount) as balance
    from Journal j join LedgerEntry e on e.journal_id = j.id
    group by j.id, j.name""")
@Synchronize({"Journal", "LedgerEntry"})
public class JournalBalance {

  @Id
  private Long journalId;

  private String name;
  private BigDecimal balance;

  protected JournalBalance() {
  }

  public String getName() {
    return name;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  @Override
  public String toString() {
    return name + " " + balance;
  }
}
