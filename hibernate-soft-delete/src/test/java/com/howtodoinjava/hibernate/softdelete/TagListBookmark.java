package com.howtodoinjava.hibernate.softdelete;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.SoftDelete;

/** Test-only: tags mapped as a List (a bag) instead of a Set. */
@Entity(name = "Bookmark")
@SoftDelete
public class TagListBookmark {

  @Id
  @GeneratedValue
  Long id;

  String title;

  @ElementCollection
  @CollectionTable(name = "bookmark_tag", joinColumns = @JoinColumn(name = "bookmark_id"))
  @Column(name = "tag")
  @SoftDelete
  List<String> tags = new ArrayList<>();

  protected TagListBookmark() {
  }

  TagListBookmark(String title) {
    this.title = title;
  }
}
