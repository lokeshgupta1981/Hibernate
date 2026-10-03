package com.howtodoinjava.hibernate.callbacks;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

/** Default listener: registered for every entity in META-INF/orm.xml. */
public class AuditListener {

  @PrePersist
  void beforeInsert(Object entity) {
    CallbackLog.add("AuditListener @PrePersist " + entity.getClass().getSimpleName());
  }

  @PreUpdate
  void beforeUpdate(Object entity) {
    CallbackLog.add("AuditListener @PreUpdate " + entity.getClass().getSimpleName());
  }

  @PreRemove
  void beforeRemove(Object entity) {
    CallbackLog.add("AuditListener @PreRemove " + entity.getClass().getSimpleName());
  }
}
