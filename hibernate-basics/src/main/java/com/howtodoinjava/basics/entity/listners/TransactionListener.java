package com.howtodoinjava.basics.entity.listners;

import com.howtodoinjava.basics.entity.TransactionEntity;
import jakarta.persistence.PostLoad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class TransactionListener {

  Logger LOG = LoggerFactory.getLogger(TransactionEntity.class);

  @PostLoad
  public void updateDuration(Object entity) {
    if (entity != null && entity instanceof TransactionEntity) {
      TransactionEntity transaction = (TransactionEntity) entity;
      LOG.info("TransactionListner.updateDuration() invoked !!");
      transaction.setDuration(Duration.between(transaction.getStartTS(),
          transaction.getEndTS()));
    }
  }
}
