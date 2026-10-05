package com.howtodoinjava.hibernate.interceptors;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class AuditLog {

  @Id
  @GeneratedValue
  private Long id;

  private String entityName;
  private Long entityId;
  private String action;
  private String property;
  private String oldValue;
  private String newValue;

  protected AuditLog() {
  }

  public AuditLog(String entityName, Long entityId, String action,
                  String property, String oldValue, String newValue) {
    this.entityName = entityName;
    this.entityId = entityId;
    this.action = action;
    this.property = property;
    this.oldValue = oldValue;
    this.newValue = newValue;
  }

  public String getEntityName() {
    return entityName;
  }

  public Long getEntityId() {
    return entityId;
  }

  public String getAction() {
    return action;
  }

  public String getProperty() {
    return property;
  }

  public String getOldValue() {
    return oldValue;
  }

  public String getNewValue() {
    return newValue;
  }

  @Override
  public String toString() {
    return action + " " + entityName + "#" + entityId
        + (property == null ? "" : " " + property + ": " + oldValue + " -> " + newValue);
  }
}
