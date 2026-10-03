package com.howtodoinjava.hibernate.softdelete;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

@Entity
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP)
public class BookmarkFolder {

  @Id
  @GeneratedValue
  private Long id;

  private String name;

  @OneToMany(mappedBy = "folder", cascade = CascadeType.ALL)
  private List<Bookmark> bookmarks = new ArrayList<>();

  protected BookmarkFolder() {
  }

  public BookmarkFolder(String name) {
    this.name = name;
  }

  public void addBookmark(Bookmark bookmark) {
    bookmarks.add(bookmark);
    bookmark.setFolder(this);
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public List<Bookmark> getBookmarks() {
    return bookmarks;
  }

  @Override
  public String toString() {
    return name;
  }
}
