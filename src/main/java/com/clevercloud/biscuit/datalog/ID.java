package com.clevercloud.biscuit.datalog;

import java.io.Serializable;

public abstract class ID implements Serializable {
   public final class Date extends ID implements Serializable {
      private final long value;

      public long value() {
         return this.value;
      }

      public Date(final long value) {
         this.value = value;
      }
   }

   public final class Integer extends ID implements Serializable {
      private final long value;

      public long value() {
         return this.value;
      }

      public Integer(final long value) {
         this.value = value;
      }
   }

   public final class Str extends ID implements Serializable {
      private final String value;

      public String value() {
         return this.value;
      }

      public Str(final String value) {
         this.value = value;
      }
   }

   public final class Symbol extends ID implements Serializable {
      private final long value;

      public long value() {
         return this.value;
      }

      public Symbol(final long value) {
         this.value = value;
      }
   }

   public final class Variable extends ID implements Serializable {
      private final long value;

      public long value() {
         return this.value;
      }

      public Variable(final long value) {
         this.value = value;
      }
   }
}
