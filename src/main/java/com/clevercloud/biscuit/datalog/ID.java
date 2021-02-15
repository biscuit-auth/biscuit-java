package com.clevercloud.biscuit.datalog;

import biscuit.format.schema.Schema;
import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.token.builder.Term;
import com.google.protobuf.ByteString;
import io.vavr.control.Either;
import static io.vavr.API.Left;
import static io.vavr.API.Right;

import java.io.Serializable;
import java.util.Objects;


public abstract class ID implements Serializable {
   public abstract boolean match(final ID other);
   public abstract Schema.IDV1 serialize();
   static public Either<Error.FormatError, ID> deserialize_enumV0(Schema.IDV0 id) {
      if(id.getKind() == Schema.IDV0.Kind.DATE) {
         return Date.deserializeV0(id);
      } else if(id.getKind() == Schema.IDV0.Kind.INTEGER) {
         return Integer.deserializeV0(id);
      } else if(id.getKind() == Schema.IDV0.Kind.STR) {
         return Str.deserializeV0(id);
      } else if(id.getKind() == Schema.IDV0.Kind.BYTES) {
         return Bytes.deserializeV0(id);
      } else if(id.getKind() == Schema.IDV0.Kind.SYMBOL) {
         return Symbol.deserializeV0(id);
      } else if(id.getKind() == Schema.IDV0.Kind.VARIABLE) {
         return Variable.deserializeV0(id);
      } else {
         return Left(new Error.FormatError.DeserializationError("invalid ID kind"));
      }
   }

   static public Either<Error.FormatError, ID> deserialize_enumV1(Schema.IDV1 id) {
      if(id.hasDate()) {
         return Date.deserializeV1(id);
      } else if(id.hasInteger()) {
         return Integer.deserializeV1(id);
      } else if(id.hasString()) {
         return Str.deserializeV1(id);
      } else if(id.hasBytes()) {
         return Bytes.deserializeV1(id);
      } else if(id.hasSymbol()) {
         return Symbol.deserializeV1(id);
      } else if(id.hasVariable()) {
         return Variable.deserializeV1(id);
      } else if(id.hasBool()) {
         return Bool.deserializeV1(id);
      } else {
         return Left(new Error.FormatError.DeserializationError("invalid ID kind"));
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
         return "@" + this.value;
      }

      public Schema.IDV1 serialize() {
         return Schema.IDV1.newBuilder()
                 .setDate(this.value).build();
      }

      static public Either<Error.FormatError, ID> deserializeV0(Schema.IDV0 id) {
         if(id.getKind() != Schema.IDV0.Kind.DATE) {
            return Left(new Error.FormatError.DeserializationError("invalid ID kind"));
         } else {
            return Right(new Date(id.getDate()));
         }
      }

      static public Either<Error.FormatError, ID> deserializeV1(Schema.IDV1 id) {
         if(!id.hasDate()) {
            return Left(new Error.FormatError.DeserializationError("invalid ID kind"));
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
         return Objects.hash(value);
      }

      @Override
      public String toString() {
         return "" + this.value;
      }

      public Schema.IDV1 serialize() {
         return Schema.IDV1.newBuilder()
                 .setInteger(this.value).build();
      }

      static public Either<Error.FormatError, ID> deserializeV0(Schema.IDV0 id) {
         if(id.getKind() != Schema.IDV0.Kind.INTEGER) {
            return Left(new Error.FormatError.DeserializationError("invalid ID kind"));
         } else {
            return Right(new Integer(id.getInteger()));
         }
      }

      static public Either<Error.FormatError, ID> deserializeV1(Schema.IDV1 id) {
         if(!id.hasInteger()) {
            return Left(new Error.FormatError.DeserializationError("invalid ID kind"));
         } else {
            return Right(new Integer(id.getInteger()));
         }
      }

      public Term toTerm(SymbolTable symbols) {
         return new Term.Integer(this.value);
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
            return this.value.equals(((Str) other).value);
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

      public Schema.IDV1 serialize() {
         return Schema.IDV1.newBuilder()
                 .setString(this.value).build();
      }

      static public Either<Error.FormatError, ID> deserializeV0(Schema.IDV0 id) {
         if(id.getKind() != Schema.IDV0.Kind.STR) {
            return Left(new Error.FormatError.DeserializationError("invalid ID kind"));
         } else {
            return Right(new Str(id.getStr()));
         }
      }

      static public Either<Error.FormatError, ID> deserializeV1(Schema.IDV1 id) {
         if(!id.hasString()) {
            return Left(new Error.FormatError.DeserializationError("invalid ID kind"));
         } else {
            return Right(new Str(id.getString()));
         }
      }

      public Term toTerm(SymbolTable symbols) {
         return new Term.Str(this.value);
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
         if (other instanceof Str) {
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
         Bytes str = (Bytes) o;
         return Objects.equals(value, str.value);
      }

      @Override
      public int hashCode() {
         return Objects.hash(value);
      }

      @Override
      public String toString() {
         return this.value.toString();
      }

      public Schema.IDV1 serialize() {
         return Schema.IDV1.newBuilder()
                 .setBytes(ByteString.EMPTY.copyFrom(this.value)).build();
      }

      static public Either<Error.FormatError, ID> deserializeV0(Schema.IDV0 id) {
         if(id.getKind() != Schema.IDV0.Kind.STR) {
            return Left(new Error.FormatError.DeserializationError("invalid ID kind"));
         } else {
            return Right(new Str(id.getStr()));
         }
      }

      static public Either<Error.FormatError, ID> deserializeV1(Schema.IDV1 id) {
         if(!id.hasBytes()) {
            return Left(new Error.FormatError.DeserializationError("invalid ID kind"));
         } else {
            return Right(new Bytes(id.getBytes().toByteArray()));
         }
      }

      public Term toTerm(SymbolTable symbols) {
         return new Term.Bytes(this.value);
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

      public Schema.IDV1 serialize() {
         return Schema.IDV1.newBuilder()
                 .setSymbol(this.value).build();
      }

      static public Either<Error.FormatError, ID> deserializeV0(Schema.IDV0 id) {
         if(id.getKind() != Schema.IDV0.Kind.SYMBOL) {
            return Left(new Error.FormatError.DeserializationError("invalid ID kind"));
         } else {
            return Right(new Symbol(id.getSymbol()));
         }
      }

      static public Either<Error.FormatError, ID> deserializeV1(Schema.IDV1 id) {
         if(!id.hasSymbol()) {
            return Left(new Error.FormatError.DeserializationError("invalid ID kind"));
         } else {
            return Right(new Symbol(id.getSymbol()));
         }
      }

      public Term toTerm(SymbolTable symbols) {
         return new Term.Symbol(symbols.print_symbol((int) this.value));
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

      @Override
      public String toString() {
         return this.value + "?";
      }

      public Schema.IDV1 serialize() {
         return Schema.IDV1.newBuilder()
                 .setVariable((int) this.value).build();
      }

      static public Either<Error.FormatError, ID> deserializeV0(Schema.IDV0 id) {
         if(id.getKind() != Schema.IDV0.Kind.VARIABLE) {
            return Left(new Error.FormatError.DeserializationError("invalid ID kind"));
         } else {
            return Right(new Variable(id.getVariable()));
         }
      }

      static public Either<Error.FormatError, ID> deserializeV1(Schema.IDV1 id) {
         if(!id.hasVariable()) {
            return Left(new Error.FormatError.DeserializationError("invalid ID kind"));
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
         return Objects.hash(value);
      }

      @Override
      public String toString() {
         return "" + this.value;
      }

      public Schema.IDV1 serialize() {
         return Schema.IDV1.newBuilder()
                 .setBool(this.value).build();
      }

      static public Either<Error.FormatError, ID> deserializeV1(Schema.IDV1 id) {
         if(!id.hasBool()) {
            return Left(new Error.FormatError.DeserializationError("invalid ID kind"));
         } else {
            return Right(new Bool(id.getBool()));
         }
      }

      public Term toTerm(SymbolTable symbols) {
         return new Term.Bool(this.value);
      }
   }
}
