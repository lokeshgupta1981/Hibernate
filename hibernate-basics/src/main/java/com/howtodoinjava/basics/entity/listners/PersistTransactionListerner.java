package com.howtodoinjava.basics.entity.listners;

import com.howtodoinjava.basics.entity.TransactionEntity;
import org.hibernate.HibernateException;
import org.hibernate.event.spi.PersistContext;
import org.hibernate.event.spi.PersistEvent;
import org.hibernate.event.spi.PersistEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class PersistTransactionListerner implements PersistEventListener {

  Logger LOG = LoggerFactory.getLogger(TransactionEntity.class);

  @Override
  public void onPersist(PersistEvent persistEvent) throws HibernateException {
    LOG.info("PersistTransactionListerner.onPersist() invoked !!");
    TransactionEntity transaction =
        (TransactionEntity) persistEvent.getObject();

    if (transaction.getDuration() == null) {
      transaction.setDuration(Duration.between(transaction.getStartTS(),
          transaction.getEndTS()));
    }
  }

  @Override
  public void onPersist(PersistEvent persistEvent,
                        PersistContext persistContext) throws HibernateException {
    //
  }
}
