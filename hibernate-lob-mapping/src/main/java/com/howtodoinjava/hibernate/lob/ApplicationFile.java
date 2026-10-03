package com.howtodoinjava.hibernate.lob;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import java.sql.Blob;
import java.sql.Clob;

/**
 * A large file attached to an application. This class is bytecode enhanced by the
 * hibernate-maven-plugin (see pom.xml), so its LAZY LOB attributes load on first access.
 */
@Entity
public class ApplicationFile {

  @Id
  @GeneratedValue
  private Long id;

  private String fileName;

  private long size;

  @Lob
  @Basic(fetch = FetchType.LAZY)
  private Blob content;

  @Lob
  @Basic(fetch = FetchType.LAZY)
  private Clob extractedText;

  protected ApplicationFile() {
  }

  public ApplicationFile(String fileName, long size) {
    this.fileName = fileName;
    this.size = size;
  }

  public Long getId() {
    return id;
  }

  public String getFileName() {
    return fileName;
  }

  public long getSize() {
    return size;
  }

  public Blob getContent() {
    return content;
  }

  public void setContent(Blob content) {
    this.content = content;
  }

  public Clob getExtractedText() {
    return extractedText;
  }

  public void setExtractedText(Clob extractedText) {
    this.extractedText = extractedText;
  }
}
