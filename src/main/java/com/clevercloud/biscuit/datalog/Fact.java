package com.clevercloud.biscuit.datalog;

import java.io.Serializable;
import java.util.Objects;

public final class Fact implements Serializable {
   private final Predicate predicate;

   public final Predicate predicate() {
      return this.predicate;
   }

   public boolean match_predicate(final Predicate predicate) {
      return this.predicate.match(predicate);
   }

   public Fact(final Predicate predicate) {
      this.predicate = predicate;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Fact fact = (Fact) o;
      return Objects.equals(predicate, fact.predicate);
   }

   @Override
   public int hashCode() {
      return Objects.hash(predicate);
   }

   @Override
   public String toString() {
      return this.predicate.toString();
   }
}
