package com.clevercloud.biscuit.datalog;

import java.io.Serializable;

public final class Fact implements Serializable {
   private final Predicate predicate;

   public Fact(final Predicate predicate) {
      this.predicate = predicate;
   }
}
