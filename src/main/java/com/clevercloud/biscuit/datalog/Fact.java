package com.clevercloud.biscuit.datalog;

import java.io.Serializable;

public final class Fact implements Serializable {
   private final Predicate predicate;

   public boolean match_predicate(final Predicate predicate) {
      return this.predicate.match(predicate);
   }

   public Fact(final Predicate predicate) {
      this.predicate = predicate;
   }
}
