package com.howtodoinjava.hibernate.softdelete;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.SoftDelete;

@Entity
@SoftDelete
public class Bookmark {

  @Id
  @GeneratedValue
  private Long id;

  private String title;
  private String url;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "folder_id")
  private BookmarkFolder folder;

  @ElementCollection
  @CollectionTable(name = "bookmark_tag", joinColumns = @JoinColumn(name = "bookmark_id"))
  @Column(name = "tag")
  @SoftDelete
  private Set<String> tags = new HashSet<>();

  protected Bookmark() {
  }

  public Bookmark(String title, String url) {
    this.title = title;
    this.url = url;
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getUrl() {
    return url;
  }

  public BookmarkFolder getFolder() {
    return folder;
  }

  void setFolder(BookmarkFolder folder) {
    this.folder = folder;
  }

  public Set<String> getTags() {
    return tags;
  }

  @Override
  public String toString() {
    return title;
  }
}
