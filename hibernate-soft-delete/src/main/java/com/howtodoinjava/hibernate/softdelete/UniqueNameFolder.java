package com.howtodoinjava.hibernate.softdelete;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

/** A folder with a plain unique name: fails when a deleted name is reused. */
@Entity(name = "BookmarkFolder")
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP)
public class UniqueNameFolder {

  @Id
  @GeneratedValue
  private Long id;

  @Column(unique = true)
  private String name;

  protected UniqueNameFolder() {
  }

  public UniqueNameFolder(String name) {
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }
}
