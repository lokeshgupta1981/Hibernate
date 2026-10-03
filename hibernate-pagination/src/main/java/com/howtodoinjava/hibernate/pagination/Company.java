package com.howtodoinjava.hibernate.pagination;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Company {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  @OneToMany(mappedBy = "company")
  private List<JobPosting> postings = new ArrayList<>();

  protected Company() {
  }

  public Company(String name) {
    this.name = name;
  }

  public void addPosting(JobPosting posting) {
    postings.add(posting);
    posting.setCompany(this);
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public List<JobPosting> getPostings() {
    return postings;
  }

  @Override
  public String toString() {
    return name;
  }
}
