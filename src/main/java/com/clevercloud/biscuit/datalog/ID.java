package com.clevercloud.biscuit.datalog;

import java.io.Serializable;
import java.util.Objects;

public abstract class ID implements Serializable {
   public abstract boolean match(final ID other);

   public final static class Date extends ID implements Serializable {
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

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;
         Date date = (Date) o;
         return value == date.value;
      }

      @Override
      public int hashCode() {
         return Objects.hash(value);
      }
   }

   public final static class Integer extends ID implements Serializable {
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

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;
         Integer integer = (Integer) o;
         return value == integer.value;
      }

      @Override
      public int hashCode() {
         return Objects.hash(value);
      }
   }

   public final static class Str extends ID implements Serializable {
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

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;
         Str str = (Str) o;
         return Objects.equals(value, str.value);
      }

      @Override
      public int hashCode() {
         return Objects.hash(value);
      }
   }

   public final static class Symbol extends ID implements Serializable {
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

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;
         Symbol symbol = (Symbol) o;
         return value == symbol.value;
      }

      @Override
      public int hashCode() {
         return Objects.hash(value);
      }
   }

   public final static class Variable extends ID implements Serializable {
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

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;
         Variable variable = (Variable) o;
         return value == variable.value;
      }

      @Override
      public int hashCode() {
         return Objects.hash(value);
      }
   }
}
