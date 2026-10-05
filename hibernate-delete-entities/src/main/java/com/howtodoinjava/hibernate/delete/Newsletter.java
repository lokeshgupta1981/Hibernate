package com.howtodoinjava.hibernate.delete;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Newsletter {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  @OneToMany(mappedBy = "newsletter")
  private List<Subscriber> subscribers = new ArrayList<>();

  protected Newsletter() {
  }

  public Newsletter(String name) {
    this.name = name;
  }

  public void addSubscriber(Subscriber subscriber) {
    subscribers.add(subscriber);
    subscriber.setNewsletter(this);
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public List<Subscriber> getSubscribers() {
    return subscribers;
  }
}
