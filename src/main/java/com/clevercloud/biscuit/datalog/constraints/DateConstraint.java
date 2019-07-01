package com.clevercloud.biscuit.datalog.constraints;

import java.io.Serializable;
import java.util.Set;

public abstract class DateConstraint implements Serializable {
   public final class Before extends DateConstraint implements Serializable {
      private final long value;

      public Before(final long value) {
         this.value = value;
      }
   }

   public final class After extends DateConstraint implements Serializable {
      private final long value;

      public After(final long value) {
         this.value = value;
      }
   }
}
