package com.clevercloud.biscuit.datalog;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
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

      @Override
      public String toString() {
         return java.util.Date.from(Instant.ofEpochSecond(this.value)).toString();
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

      @Override
      public String toString() {
         return "" + this.value;
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

      @Override
      public String toString() {
         return this.value;
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

      @Override
      public String toString() {
         return "#" + this.value;
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

      public Variable(final String name) {
         long value = 0;
         try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] res = digest.digest(name.getBytes(StandardCharsets.UTF_8));
            System.out.println("name was encoded to " + Byte.toUnsignedLong(res[0]) + " " + Byte.toUnsignedLong(res[1]) + " " + Byte.toUnsignedLong(res[2]) + " " + Byte.toUnsignedLong(res[3]));
            value = Byte.toUnsignedLong(res[0]) + (Byte.toUnsignedLong(res[1]) << 8) + (Byte.toUnsignedLong(res[2]) << 16) + (Byte.toUnsignedLong(res[3]) << 24);
            System.out.println("name value is " + value);
         } catch (NoSuchAlgorithmException e) {
            assert e == null;
         }
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

      @Override
      public String toString() {
         return this.value + "?";
      }
   }
}
