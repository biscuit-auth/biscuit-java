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
   public abstract Schema.SymbolConstraintV1 serialize();

   static public Either<Error.FormatError, SymbolConstraint> deserialize_enumV0(Schema.SymbolConstraintV0 c) {
      if (c.getKind() == Schema.SymbolConstraintV0.Kind.IN) {
         return InSet.deserializeV0(c);
      } else if (c.getKind() == Schema.SymbolConstraintV0.Kind.IN) {
         return NotInSet.deserializeV0(c);
      } else {
         return Left(new Error.FormatError.DeserializationError("invalid Symbol constraint kind"));
      }
   }

   static public Either<Error.FormatError, SymbolConstraint> deserialize_enumV1(Schema.SymbolConstraintV1 c) {
      if (c.hasInSet()) {
         return InSet.deserializeV1(c);
      } else if (c.hasNotInSet()) {
         return NotInSet.deserializeV1(c);
      } else {
         return Left(new Error.FormatError.DeserializationError("invalid Symbol constraint kind"));
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

      public Schema.SymbolConstraintV1 serialize() {
         Schema.SymbolConstraintV1.Builder b = Schema.SymbolConstraintV1.newBuilder();
         Schema.SymbolSet.Builder s = Schema.SymbolSet.newBuilder();

         for (Long l: this.value) {
            s.addSet(l);
         }
         b.setInSet(s);

         return b.build();
      }

      static public Either<Error.FormatError, SymbolConstraint> deserializeV0(Schema.SymbolConstraintV0 i) {
         Set<Long> values = new HashSet<>();
         for (long l: i.getInSetList()) {
            values.add(l);
         }
         if(values.isEmpty()) {
            return Left(new Error.FormatError.DeserializationError("invalid Symbol constraint"));
         } else {
            return Right(new InSet(values));
         }
      }

      static public Either<Error.FormatError, SymbolConstraint> deserializeV1(Schema.SymbolConstraintV1 i) {
         Set<Long> values = new HashSet<>();
         Schema.SymbolSet s = i.getInSet();
         for (Long l: s.getSetList()) {
            values.add(l);
         }
         if(values.isEmpty()) {
            return Left(new Error.FormatError.DeserializationError("invalid Symbol constraint"));
         } else {
            return Right(new SymbolConstraint.InSet(values));
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

      public Schema.SymbolConstraintV1 serialize() {
         Schema.SymbolConstraintV1.Builder b = Schema.SymbolConstraintV1.newBuilder();
         Schema.SymbolSet.Builder s = Schema.SymbolSet.newBuilder();

         for (Long l: this.value) {
            s.addSet(l);
         }
         b.setNotInSet(s);

         return b.build();
      }

      static public Either<Error.FormatError, SymbolConstraint> deserializeV0(Schema.SymbolConstraintV0 i) {
         Set<Long> values = new HashSet<>();
         for (long l: i.getNotInSetList()) {
            values.add(l);
         }
         if(values.isEmpty()) {
            return Left(new Error.FormatError.DeserializationError("invalid Symbol constraint"));
         } else {
            return Right(new NotInSet(values));
         }
      }

      static public Either<Error.FormatError, SymbolConstraint> deserializeV1(Schema.SymbolConstraintV1 i) {
         Set<Long> values = new HashSet<>();
         Schema.SymbolSet s = i.getNotInSet();
         for (Long l: s.getSetList()) {
            values.add(l);
         }
         if(values.isEmpty()) {
            return Left(new Error.FormatError.DeserializationError("invalid Symbol constraint"));
         } else {
            return Right(new SymbolConstraint.NotInSet(values));
         }
      }
   }
}
