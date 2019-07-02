package com.clevercloud.biscuit.datalog.constraints;

import java.io.Serializable;
import java.util.Set;

public abstract class StrConstraint implements Serializable {
   public abstract boolean check(String value);

   public final class Prefix extends StrConstraint implements Serializable {
      private final String value;

      public boolean check(String value) {
         return value.startsWith(this.value);
      }

      public Prefix(final String value) {
         this.value = value;
      }
   }

   public final class Suffix extends StrConstraint implements Serializable {
      private final String value;

      public boolean check(String value) {
         return value.endsWith(this.value);
      }

      public Suffix(final String value) {
         this.value = value;
      }
   }

   public final class Equal extends StrConstraint implements Serializable {
      private final String value;

      public boolean check(String value) {
         return this.value.equals(value);
      }

      public Equal(final String value) {
         this.value = value;
      }
   }

   public final class InSet extends StrConstraint implements Serializable {
      private final Set<String> value;

      public boolean check(String value) {
         return this.value.contains(value);
      }

      public InSet(final Set<String> value) {
         this.value = value;
      }
   }

   public final class NotInSet extends StrConstraint implements Serializable {
      private final Set<String> value;

      public boolean check(String value) {
         return !this.value.contains(value);
      }

      public NotInSet(final Set<String> value) {
         this.value = value;
      }
   }
}
