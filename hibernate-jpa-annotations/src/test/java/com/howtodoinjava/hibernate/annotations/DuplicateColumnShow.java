package com.howtodoinjava.hibernate.annotations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/** Maps play_id twice without insertable = false, updatable = false. Used only to reproduce the error. */
@Entity
public class DuplicateColumnShow {

  @Id
  private Long id;

  @ManyToOne
  @JoinColumn(name = "play_id")
  private Play play;

  @Column(name = "play_id")
  private Long playId;
}
