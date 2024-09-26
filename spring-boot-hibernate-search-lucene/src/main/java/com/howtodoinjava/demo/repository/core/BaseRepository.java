package com.howtodoinjava.demo.repository.core;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;

@NoRepositoryBean
public interface BaseRepository<T, ID extends Serializable>
    extends JpaRepository<T, ID>, SearchRepository<T, ID> {
}
