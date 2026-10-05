package com.howtodoinjava.hibernate.callbacks;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;

/** Registered in META-INF/services/org.hibernate.integrator.spi.Integrator. */
public class ClaimEventsIntegrator implements Integrator {

  @Override
  public void integrate(Metadata metadata, BootstrapContext bootstrapContext,
                        SessionFactoryImplementor sessionFactory) {
    sessionFactory.getEventListenerRegistry()
        .appendListeners(EventType.POST_COMMIT_INSERT, new ClaimCommitListener());
  }
}
