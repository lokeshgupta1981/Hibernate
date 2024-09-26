package com.howtodoinjava.demo.exception;

public class ResourceNotFoundException extends Exception {

  private static final long serialVersionUID = 1L;

  public ResourceNotFoundException(String message) {
    super(message);
  }

  public ResourceNotFoundException(String message, Throwable th) {
    super(message, th);
  }
}
