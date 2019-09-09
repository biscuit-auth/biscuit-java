package com.clevercloud.biscuit.datalog.constraints;

import biscuit.format.schema.Schema;
import com.clevercloud.biscuit.error.Error;
import io.vavr.control.Either;

import java.io.Serializable;
import java.util.Set;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

public abstract class DateConstraint implements Serializable {
   public abstract boolean check(final long value);
   public abstract Schema.DateConstraint serialize();

   static public Either<Error.FormatError, DateConstraint> deserialize_enum(Schema.DateConstraint c) {
      if (c.getKind() == Schema.DateConstraint.Kind.BEFORE) {
         return Before.deserialize(c);
      } else if (c.getKind() == Schema.DateConstraint.Kind.AFTER) {
         return After.deserialize(c);
      } else {
         return Left(new Error().new FormatError().new DeserializationError("invalid date constraint kind"));
      }
   }

   public static final class Before extends DateConstraint implements Serializable {
      private final long value;

      public boolean check(final long value) {
         return this.value > value;
      }

      public Before(final long value) {
         this.value = value;
      }

      @Override
      public String toString() {
         return "<= " + this.value;
      }

      public Schema.DateConstraint serialize() {
         return Schema.DateConstraint.newBuilder()
                 .setKind(Schema.DateConstraint.Kind.BEFORE)
                 .setBefore(this.value).build();
      }

      static public Either<Error.FormatError, DateConstraint> deserialize(Schema.DateConstraint i) {
         if(!i.hasBefore()) {
            return Left(new Error().new FormatError().new DeserializationError("invalid Date constraint"));
         } else {
            return Right(new Before(i.getBefore()));
         }
      }
   }

   public static final class After extends DateConstraint implements Serializable {
      private final long value;

      public boolean check(final long value) {
         return this.value < value;
      }

      public After(final long value) {
         this.value = value;
      }

      @Override
      public String toString() {
         return ">= " + this.value;
      }

      public Schema.DateConstraint serialize() {
         return Schema.DateConstraint.newBuilder()
                 .setKind(Schema.DateConstraint.Kind.AFTER)
                 .setAfter(this.value).build();
      }

      static public Either<Error.FormatError, DateConstraint> deserialize(Schema.DateConstraint i) {
         if(!i.hasAfter()) {
            return Left(new Error().new FormatError().new DeserializationError("invalid Date constraint"));
         } else {
            return Right(new After(i.getAfter()));
         }
      }
   }
}
