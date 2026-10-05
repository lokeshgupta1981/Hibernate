package com.howtodoinjava.hibernate.callbacks;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PreUpdate;
import java.math.BigDecimal;

/**
 * Callbacks that break the rules of the Jakarta Persistence specification.
 * Registered as a second default listener in META-INF/orm-risky.xml, used only by Database.createRisky().
 * Each method does nothing unless the demo or a test switches its mode on.
 */
public class RiskyListener {

  public enum Mode { OFF, QUERY_IN_PRE_UPDATE, PERSIST_IN_POST_PERSIST, CHANGE_IN_POST_PERSIST, CHANGE_IN_POST_UPDATE }

  public static Mode mode = Mode.OFF;

  /** In a Spring application this would be an injected EntityManager. */
  public static EntityManager em;

  @PreUpdate
  void checkBudget(Object entity) {
    if (mode == Mode.QUERY_IN_PRE_UPDATE && entity instanceof ExpenseClaim) {
      BigDecimal total = em.createQuery("select sum(c.amount) from ExpenseClaim c", BigDecimal.class)
          .getSingleResult();
      CallbackLog.add("RiskyListener total = " + total);
    }
  }

  @PostPersist
  void afterSubmit(Object entity) {
    if (!(entity instanceof ExpenseClaim claim)) {
      return;
    }
    if (mode == Mode.PERSIST_IN_POST_PERSIST) {
      em.persist(new ClaimHistory(claim.getId(), "submitted"));
    } else if (mode == Mode.CHANGE_IN_POST_PERSIST) {
      claim.setDescription(claim.getDescription() + " #" + claim.getId());
    }
  }

  @PostUpdate
  void afterUpdate(Object entity) {
    if (mode == Mode.CHANGE_IN_POST_UPDATE && entity instanceof ExpenseClaim claim) {
      claim.setDescription(claim.getDescription() + " (checked)");
    }
  }
}
