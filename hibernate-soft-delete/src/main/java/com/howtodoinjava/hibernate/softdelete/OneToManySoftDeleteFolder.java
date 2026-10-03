package com.howtodoinjava.hibernate.softdelete;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.SoftDelete;

/** Wrong mapping: @SoftDelete is not allowed on @OneToMany. Hibernate refuses to start. */
@Entity
public class OneToManySoftDeleteFolder {

  @Id
  @GeneratedValue
  private Long id;

  private String name;

  @OneToMany
  @JoinColumn(name = "folder_id")
  @SoftDelete
  private List<Bookmark> bookmarks = new ArrayList<>();
}
