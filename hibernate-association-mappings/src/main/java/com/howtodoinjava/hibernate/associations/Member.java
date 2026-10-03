package com.howtodoinjava.hibernate.associations;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
public class Member {

  @Id
  @GeneratedValue
  private Long id;

  private String name;

  // One-to-one, inverse side: the foreign key is MembershipCard.member_id
  @OneToOne(mappedBy = "member", cascade = CascadeType.ALL)
  private MembershipCard card;

  // One-to-many, inverse side: the foreign key is Workout.member_id
  @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Workout> workouts = new ArrayList<>();

  // Many-to-many, owning side: Member writes the member_class join table
  @ManyToMany
  @JoinTable(name = "member_class",
      joinColumns = @JoinColumn(name = "member_id"),
      inverseJoinColumns = @JoinColumn(name = "class_id"))
  private Set<FitnessClass> classes = new HashSet<>();

  protected Member() {
  }

  public Member(String name) {
    this.name = name;
  }

  public void setCard(MembershipCard card) {
    this.card = card;
    card.setMember(this);
  }

  public void addWorkout(Workout workout) {
    workouts.add(workout);
    workout.setMember(this);
  }

  public void removeWorkout(Workout workout) {
    workouts.remove(workout);
    workout.setMember(null);
  }

  public void joinClass(FitnessClass fitnessClass) {
    classes.add(fitnessClass);
    fitnessClass.getMembers().add(this);
  }

  public void leaveClass(FitnessClass fitnessClass) {
    classes.remove(fitnessClass);
    fitnessClass.getMembers().remove(this);
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public MembershipCard getCard() {
    return card;
  }

  public List<Workout> getWorkouts() {
    return workouts;
  }

  public Set<FitnessClass> getClasses() {
    return classes;
  }
}
