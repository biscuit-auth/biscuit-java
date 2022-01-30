package com.clevercloud.biscuit.datalog;

import biscuit.format.schema.Schema;
import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.token.builder.Term;
import com.google.protobuf.ByteString;
import io.vavr.control.Either;
import static io.vavr.API.Left;
import static io.vavr.API.Right;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;


public abstract class ID implements Serializable {
   public abstract boolean match(final ID other);
   public abstract Schema.IDV2 serialize();

   static public Either<Error.FormatError, ID> deserialize_enumV2(Schema.IDV2 id) {
      if(id.hasDate()) {
         return Date.deserializeV2(id);
      } else if(id.hasInteger()) {
         return Integer.deserializeV2(id);
      } else if(id.hasString()) {
         return Str.deserializeV2(id);
      } else if(id.hasBytes()) {
         return Bytes.deserializeV2(id);
      } else if(id.hasVariable()) {
         return Variable.deserializeV2(id);
      } else if(id.hasBool()) {
         return Bool.deserializeV2(id);
      } else if(id.hasSet()) {
         return Set.deserializeV2(id);
      } else {
         return Left(new Error.FormatError.DeserializationError("invalid ID kind: id.getKind()"));
      }
   }

   public abstract Term toTerm(SymbolTable symbols);

   public final static class Date extends ID implements Serializable {
      private final long value;

      public long value() {
         return this.value;
      }

      public boolean match(final ID other) {
         if (other instanceof Variable) {
            return true;
         } else {
            return this.equals(other);
         }
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
         return (int) (value ^ (value >>> 32));
      }

      @Override
      public String toString() {
         return "@" + this.value;
      }

      public Schema.IDV2 serialize() {
         return Schema.IDV2.newBuilder()
                 .setDate(this.value).build();
      }

      static public Either<Error.FormatError, ID> deserializeV2(Schema.IDV2 id) {
         if(!id.hasDate()) {
            return Left(new Error.FormatError.DeserializationError("invalid ID kind, expected date"));
         } else {
            return Right(new Date(id.getDate()));
         }
      }

      public Term toTerm(SymbolTable symbols) {
         return new Term.Date(this.value);
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
         return (int) (value ^ (value >>> 32));
      }

      @Override
      public String toString() {
         return "" + this.value;
      }

      public Schema.IDV2 serialize() {
         return Schema.IDV2.newBuilder()
                 .setInteger(this.value).build();
      }

      static public Either<Error.FormatError, ID> deserializeV2(Schema.IDV2 id) {
         if(!id.hasInteger()) {
            return Left(new Error.FormatError.DeserializationError("invalid ID kind, expected integer"));
         } else {
            return Right(new Integer(id.getInteger()));
         }
      }

      public Term toTerm(SymbolTable symbols) {
         return new Term.Integer(this.value);
      }
   }

   public final static class Bytes extends ID implements Serializable {
      private final byte[] value;

      public byte[] value() {
         return this.value;
      }

      public boolean match(final ID other) {
         if (other instanceof Variable) {
            return true;
         }
         if (other instanceof Bytes) {
            return this.value.equals(((Bytes) other).value);
         }
         return false;
      }

      public Bytes(final byte[] value) {
         this.value = value;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;

         Bytes bytes = (Bytes) o;

         return Arrays.equals(value, bytes.value);
      }

      @Override
      public int hashCode() {
         return Arrays.hashCode(value);
      }

      @Override
      public String toString() {
         return this.value.toString();
      }

      public Schema.IDV2 serialize() {
         return Schema.IDV2.newBuilder()
                 .setBytes(ByteString.EMPTY.copyFrom(this.value)).build();
      }

      static public Either<Error.FormatError, ID> deserializeV2(Schema.IDV2 id) {
         if(!id.hasBytes()) {
            return Left(new Error.FormatError.DeserializationError("invalid ID kind, expected byte array"));
         } else {
            return Right(new Bytes(id.getBytes().toByteArray()));
         }
      }

      public Term toTerm(SymbolTable symbols) {
         return new Term.Bytes(this.value);
      }
   }

   public final static class Str extends ID implements Serializable {
      private final long value;

      public long value() {
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

      public Str(final long value) {
         this.value = value;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;

         Str s = (Str) o;

         if (value != s.value) return false;

         return true;
      }

      @Override
      public int hashCode() {
         return (int) (value ^ (value >>> 32));
      }

      public Schema.IDV2 serialize() {
         return Schema.IDV2.newBuilder()
                 .setString(this.value).build();
      }

      static public Either<Error.FormatError, ID> deserializeV2(Schema.IDV2 id) {
         if(!id.hasString()) {
            return Left(new Error.FormatError.DeserializationError("invalid ID kind, expected string"));
         } else {
            return Right(new Str(id.getString()));
         }
      }

      public Term toTerm(SymbolTable symbols) {
         return new Term.Str(symbols.print_symbol((int) this.value));
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
         return (int) (value ^ (value >>> 32));
      }

      @Override
      public String toString() {
         return this.value + "?";
      }

      public Schema.IDV2 serialize() {
         return Schema.IDV2.newBuilder()
                 .setVariable((int) this.value).build();
      }

      static public Either<Error.FormatError, ID> deserializeV2(Schema.IDV2 id) {
         if(!id.hasVariable()) {
            return Left(new Error.FormatError.DeserializationError("invalid ID kind, expected variable"));
         } else {
            return Right(new Variable(id.getVariable()));
         }
      }

      public Term toTerm(SymbolTable symbols) {
         return new Term.Variable(symbols.print_symbol((int) this.value));
      }
   }

   public final static class Bool extends ID implements Serializable {
      private final boolean value;

      public boolean value() {
         return this.value;
      }

      public boolean match(final ID other) {
         if (other instanceof Variable) {
            return true;
         }
         if (other instanceof Bool) {
            return this.value == ((Bool) other).value;
         }
         return false;
      }

      public Bool(final boolean value) {
         this.value = value;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;

         Bool bool = (Bool) o;

         return value == bool.value;
      }

      @Override
      public int hashCode() {
         return (value ? 1 : 0);
      }

      @Override
      public String toString() {
         return "" + this.value;
      }

      public Schema.IDV2 serialize() {
         return Schema.IDV2.newBuilder()
                 .setBool(this.value).build();
      }

      static public Either<Error.FormatError, ID> deserializeV2(Schema.IDV2 id) {
         if(!id.hasBool()) {
            return Left(new Error.FormatError.DeserializationError("invalid ID kind, expected boolean"));
         } else {
            return Right(new Bool(id.getBool()));
         }
      }

      public Term toTerm(SymbolTable symbols) {
         return new Term.Bool(this.value);
      }
   }

   public final static class Set extends ID implements Serializable {
      private final HashSet<ID> value;

      public HashSet<ID> value() {
         return this.value;
      }

      public boolean match(final ID other) {
         if (other instanceof Variable) {
            return true;
         }
         if (other instanceof Set) {
            return this.value.equals(((Set) other).value);
         }
         return false;
      }

      public Set(final HashSet<ID> value) {
         this.value = value;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;

         Set set = (Set) o;

         return value.equals(set.value);
      }

      @Override
      public int hashCode() {
         return value.hashCode();
      }

      @Override
      public String toString() {
         return "" +
                 value;
      }

      public Schema.IDV2 serialize() {
         Schema.IDSet.Builder s = Schema.IDSet.newBuilder();

         for (ID l: this.value) {
            s.addSet(l.serialize());
         }

         return Schema.IDV2.newBuilder()
                 .setSet(s).build();
      }

      static public Either<Error.FormatError, ID> deserializeV2(Schema.IDV2 id) {
         if(!id.hasSet()) {
            return Left(new Error.FormatError.DeserializationError("invalid ID kind, expected set"));
         } else {
            java.util.HashSet<ID> values = new HashSet<>();
            Schema.IDSet s = id.getSet();

            for (Schema.IDV2 l: s.getSetList()) {
               Either<Error.FormatError, ID> res = ID.deserialize_enumV2(l);
               if(res.isLeft()) {
                  Error.FormatError e = res.getLeft();
                  return Left(e);
               } else {
                  ID value = res.get();

                  if(value instanceof Variable) {
                     return Left(new Error.FormatError.DeserializationError("sets cannot contain variables"));
                  }

                  values.add(value);
               }
            }

            if(values.isEmpty()) {
               return Left(new Error.FormatError.DeserializationError("invalid Set value"));
            } else {
               return Right(new Set(values));
            }
         }
      }

      public Term toTerm(SymbolTable symbols) {
         HashSet<Term> s = new HashSet<>();

         for(ID i: this.value) {
            s.add(i.toTerm(symbols));
         }

         return new Term.Set(s);
      }
   }
}
