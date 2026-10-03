package com.howtodoinjava.hibernate.softdelete;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.SoftDelete;

/** Test-only: a soft-deleted bookmark whose folder is NOT soft-deleted. */
@Entity
@SoftDelete
public class PlainFolderBookmark {

  @Id
  @GeneratedValue
  Long id;

  String title;

  @ManyToOne(fetch = FetchType.LAZY)
  PlainFolder folder;

  protected PlainFolderBookmark() {
  }

  PlainFolderBookmark(String title, PlainFolder folder) {
    this.title = title;
    this.folder = folder;
  }
}
