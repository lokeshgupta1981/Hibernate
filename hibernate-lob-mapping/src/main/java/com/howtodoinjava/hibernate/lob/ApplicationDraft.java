package com.howtodoinjava.hibernate.lob;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/** The same fields as JobApplication, but without @Lob: the default column sizes apply. */
@Entity
public class ApplicationDraft {

  @Id
  @GeneratedValue
  private Long id;

  private byte[] resume;

  private String coverLetter;

  protected ApplicationDraft() {
  }

  public ApplicationDraft(byte[] resume, String coverLetter) {
    this.resume = resume;
    this.coverLetter = coverLetter;
  }

  public Long getId() {
    return id;
  }

  public byte[] getResume() {
    return resume;
  }

  public String getCoverLetter() {
    return coverLetter;
  }
}
