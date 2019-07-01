package com.clevercloud.biscuit.datalog.constraints;

import java.io.Serializable;
import java.util.Set;

public abstract class IntConstraint implements Serializable {
   public final class Equal extends IntConstraint implements Serializable {
      private final long value;

      public Equal(final long value) {
         this.value = value;
      }
   }

   public final class Greater extends IntConstraint implements Serializable {
      private final long value;

      public Greater(final long value) {
         this.value = value;
      }
   }

   public final class GreaterOrEqual extends IntConstraint implements Serializable {
      private final long value;

      public GreaterOrEqual(final long value) {
         this.value = value;
      }
   }

   public final class Lower extends IntConstraint implements Serializable {
      private final long value;

      public Lower(final long value) {
         this.value = value;
      }
   }

   public final class LowerOrEqual extends IntConstraint implements Serializable {
      private final long value;

      public LowerOrEqual(final long value) {
         this.value = value;
      }
   }

   public final class InSet extends IntConstraint implements Serializable {
      private final Set<Long> value;

      public InSet(final Set<Long> value) {
         this.value = value;
      }
   }

   public final class NotInSet extends IntConstraint implements Serializable {
      private final Set<Long> value;

      public NotInSet(final Set<Long> value) {
         this.value = value;
      }
   }
}
