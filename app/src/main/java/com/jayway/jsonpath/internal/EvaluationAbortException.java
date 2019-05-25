package com.jayway.jsonpath.internal;

public class EvaluationAbortException extends RuntimeException {

  private static final long serialVersionUID = 4419305302960432348L;

  // this is just a marker exception to abort evaluation, we don't care about
  // the stack
  @Override
  public Throwable fillInStackTrace() {
    return this;
  }
}
