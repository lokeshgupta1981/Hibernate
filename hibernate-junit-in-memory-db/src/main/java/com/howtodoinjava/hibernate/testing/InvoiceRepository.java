package com.howtodoinjava.hibernate.testing;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class InvoiceRepository {

  private final EntityManager em;

  public InvoiceRepository(EntityManager em) {
    this.em = em;
  }

  public Invoice save(Invoice invoice) {
    em.persist(invoice);
    return invoice;
  }

  public Optional<Invoice> findById(Long id) {
    return Optional.ofNullable(em.find(Invoice.class, id));
  }

  public List<Invoice> findUnpaid() {
    return em.createQuery("from Invoice where paid = false order by issuedOn", Invoice.class)
        .getResultList();
  }

  public BigDecimal outstandingFor(String client) {
    return em.createQuery(
            "select coalesce(sum(amount), 0) from Invoice where client = :client and paid = false",
            BigDecimal.class)
        .setParameter("client", client)
        .getSingleResult();
  }

  public int markAllPaid(String client) {
    return em.createQuery("update Invoice set paid = true where client = :client")
        .setParameter("client", client)
        .executeUpdate();
  }
}
