package com.clevercloud.biscuit.datalog.constraints;

import java.io.Serializable;
import java.util.Set;

public abstract class SymbolConstraint implements Serializable {
   public abstract boolean check(long value);

   public final class InSet extends SymbolConstraint implements Serializable {
      private final Set<Long> value;

      public boolean check(long value) {
         return this.value.contains(value);
      }

      public InSet(final Set<Long> value) {
         this.value = value;
      }
   }

   public final class NotInSet extends SymbolConstraint implements Serializable {
      private final Set<Long> value;

      public boolean check(long value) {
         return !this.value.contains(value);
      }

      public NotInSet(final Set<Long> value) {
         this.value = value;
      }
   }
}
