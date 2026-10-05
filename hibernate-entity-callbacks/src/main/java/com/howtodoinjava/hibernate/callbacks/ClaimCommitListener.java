package com.howtodoinjava.hibernate.callbacks;

import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.persister.entity.EntityPersister;

/** Hibernate event listener that runs after the transaction commits. */
public class ClaimCommitListener implements PostCommitInsertEventListener {

  @Override
  public void onPostInsert(PostInsertEvent event) {
    if (event.getEntity() instanceof ExpenseClaim claim) {
      CallbackLog.add("ClaimCommitListener committed claim " + claim.getId());
    }
  }

  @Override
  public void onPostInsertCommitFailed(PostInsertEvent event) {
    CallbackLog.add("ClaimCommitListener commit failed for " + event.getId());
  }

  @Override
  public boolean requiresPostCommitHandling(EntityPersister persister) {
    return true;
  }
}
