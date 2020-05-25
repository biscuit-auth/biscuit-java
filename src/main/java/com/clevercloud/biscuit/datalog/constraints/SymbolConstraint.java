package com.clevercloud.biscuit.datalog.constraints;

import biscuit.format.schema.Schema;
import com.clevercloud.biscuit.error.Error;
import io.vavr.control.Either;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

public abstract class SymbolConstraint implements Serializable {
   public abstract boolean check(final long value);
   public abstract Schema.SymbolConstraint serialize();

   static public Either<Error.FormatError, SymbolConstraint> deserialize_enum(Schema.SymbolConstraint c) {
      if (c.getKind() == Schema.SymbolConstraint.Kind.IN) {
         return InSet.deserialize(c);
      } else if (c.getKind() == Schema.SymbolConstraint.Kind.IN) {
         return NotInSet.deserialize(c);
      } else {
         return Left(new Error().new FormatError().new DeserializationError("invalid Symbol constraint kind"));
      }
   }

   public static final class InSet extends SymbolConstraint implements Serializable {
      public final Set<Long> value;

      public boolean check(final long value) {
         return this.value.contains(value);
      }

      public InSet(final Set<Long> value) {
         this.value = value;
      }

      @Override
      public String toString() {
         return "in " + this.value;
      }

      public Schema.SymbolConstraint serialize() {
         Schema.SymbolConstraint.Builder b = Schema.SymbolConstraint.newBuilder()
                 .setKind(Schema.SymbolConstraint.Kind.IN);
         for (Long l: this.value) {
            b.addInSet(l);
         }
         return b.build();
      }

      static public Either<Error.FormatError, SymbolConstraint> deserialize(Schema.SymbolConstraint i) {
         Set<Long> values = new HashSet<>();
         for (long l: i.getInSetList()) {
            values.add(l);
         }
         if(values.isEmpty()) {
            return Left(new Error().new FormatError().new DeserializationError("invalid Symbol constraint"));
         } else {
            return Right(new InSet(values));
         }
      }
   }

   public static final class NotInSet extends SymbolConstraint implements Serializable {
      public final Set<Long> value;

      public boolean check(final long value) {
         return !this.value.contains(value);
      }

      public NotInSet(final Set<Long> value) {
         this.value = value;
      }

      @Override
      public String toString() {
         return "not in " + this.value;
      }

      public Schema.SymbolConstraint serialize() {
         Schema.SymbolConstraint.Builder b = Schema.SymbolConstraint.newBuilder()
                 .setKind(Schema.SymbolConstraint.Kind.NOT_IN);
         for (Long l: this.value) {
            b.addNotInSet(l);
         }
         return b.build();
      }

      static public Either<Error.FormatError, SymbolConstraint> deserialize(Schema.SymbolConstraint i) {
         Set<Long> values = new HashSet<>();
         for (long l: i.getNotInSetList()) {
            values.add(l);
         }
         if(values.isEmpty()) {
            return Left(new Error().new FormatError().new DeserializationError("invalid Symbol constraint"));
         } else {
            return Right(new NotInSet(values));
         }
      }
   }
}
