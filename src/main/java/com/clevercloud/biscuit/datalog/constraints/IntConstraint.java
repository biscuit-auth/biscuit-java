package com.clevercloud.biscuit.datalog.constraints;

import java.io.Serializable;
import java.util.Set;

public abstract class IntConstraint implements Serializable {
   public abstract boolean check(final long value);

   public final class Equal extends IntConstraint implements Serializable {
      private final long value;

      public boolean check(final long value) {
         return this.value == value;
      }

      public Equal(final long value) {
         this.value = value;
      }
   }

   public final class Greater extends IntConstraint implements Serializable {
      private final long value;

      public boolean check(final long value) {
         return this.value < value;
      }

      public Greater(final long value) {
         this.value = value;
      }
   }

   public final class GreaterOrEqual extends IntConstraint implements Serializable {
      private final long value;

      public boolean check(final long value) {
         return this.value <= value;
      }

      public GreaterOrEqual(final long value) {
         this.value = value;
      }
   }

   public final class Lower extends IntConstraint implements Serializable {
      private final long value;

      public boolean check(final long value) {
         return this.value > value;
      }

      public Lower(final long value) {
         this.value = value;
      }
   }

   public final class LowerOrEqual extends IntConstraint implements Serializable {
      private final long value;

      public boolean check(final long value) {
         return this.value >= value;
      }

      public LowerOrEqual(final long value) {
         this.value = value;
      }
   }

   public final class InSet extends IntConstraint implements Serializable {
      private final Set<Long> value;

      public boolean check(final long value) {
         return this.value.contains(value);
      }

      public InSet(final Set<Long> value) {
         this.value = value;
      }
   }

   public final class NotInSet extends IntConstraint implements Serializable {
      private final Set<Long> value;

      public boolean check(final long value) {
         return !this.value.contains(value);
      }

      public NotInSet(final Set<Long> value) {
         this.value = value;
      }
   }
}
