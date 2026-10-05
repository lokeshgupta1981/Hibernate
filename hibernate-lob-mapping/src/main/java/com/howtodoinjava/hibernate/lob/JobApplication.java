package com.howtodoinjava.hibernate.lob;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

@Entity
public class JobApplication {

  @Id
  @GeneratedValue
  private Long id;

  private String candidateName;

  @Lob
  @Basic(fetch = FetchType.LAZY)   // ignored without bytecode enhancement
  private byte[] resume;

  @Lob
  private String coverLetter;

  protected JobApplication() {
  }

  public JobApplication(String candidateName) {
    this.candidateName = candidateName;
  }

  public Long getId() {
    return id;
  }

  public String getCandidateName() {
    return candidateName;
  }

  public byte[] getResume() {
    return resume;
  }

  public void setResume(byte[] resume) {
    this.resume = resume;
  }

  public String getCoverLetter() {
    return coverLetter;
  }

  public void setCoverLetter(String coverLetter) {
    this.coverLetter = coverLetter;
  }
}
