package com.clevercloud.biscuit.datalog;

import java.io.Serializable;

public abstract class ID implements Serializable {
   public abstract boolean match(final ID other);

   public final class Date extends ID implements Serializable {
      private final long value;

      public long value() {
         return this.value;
      }

      public boolean match(final ID other) {
         if (other instanceof Variable) {
            return true;
         }
         return false;
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

      public boolean match(final ID other) {
         if (other instanceof Variable) {
            return true;
         }
         if (other instanceof Integer) {
            return this.value == ((Integer) other).value;
         }
         return false;
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

      public boolean match(final ID other) {
         if (other instanceof Variable) {
            return true;
         }
         if (other instanceof Str) {
            return this.value == ((Str) other).value;
         }
         return false;
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

      public boolean match(final ID other) {
         if (other instanceof Variable) {
            return true;
         }
         if (other instanceof Symbol) {
            return this.value == ((Symbol) other).value;
         }
         return false;
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

      public boolean match(final ID other) {
         return true;
      }

      public Variable(final long value) {
         this.value = value;
      }
   }
}
