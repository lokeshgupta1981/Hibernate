package com.howtodoinjava.hibernate.initialize;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Team {

  @Id
  @GeneratedValue
  private Long id;

  private String name;

  @OneToMany(mappedBy = "team")
  private List<Player> players = new ArrayList<>();

  protected Team() {
  }

  public Team(String name) {
    this.name = name;
  }

  public void addPlayer(Player player) {
    players.add(player);
    player.setTeam(this);
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public List<Player> getPlayers() {
    return players;
  }

  @Override
  public String toString() {
    return "Team(" + name + ")";
  }
}
