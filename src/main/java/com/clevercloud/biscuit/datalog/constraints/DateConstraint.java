package com.clevercloud.biscuit.datalog.constraints;

import biscuit.format.schema.Schema;
import com.clevercloud.biscuit.error.Error;
import io.vavr.control.Either;

import java.io.Serializable;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

public abstract class DateConstraint implements Serializable {
   public abstract boolean check(final long value);
   public abstract Schema.DateConstraintV1 serialize();

   static public Either<Error.FormatError, DateConstraint> deserialize_enumV0(Schema.DateConstraintV0 c) {
      if (c.getKind() == Schema.DateConstraintV0.Kind.BEFORE) {
         return Before.deserializeV0(c);
      } else if (c.getKind() == Schema.DateConstraintV0.Kind.AFTER) {
         return After.deserializeV0(c);
      } else {
         return Left(new Error.FormatError.DeserializationError("invalid date constraint kind"));
      }
   }

   static public Either<Error.FormatError, DateConstraint> deserialize_enumV1(Schema.DateConstraintV1 c) {
      if (c.hasBefore()) {
         return Before.deserializeV1(c);
      } else if (c.hasAfter()) {
         return After.deserializeV1(c);
      } else {
         return Left(new Error.FormatError.DeserializationError("invalid date constraint kind"));
      }
   }

   public static final class Before extends DateConstraint implements Serializable {
      private final long value;

      public boolean check(final long value) {
         return this.value >= value;
      }

      public Before(final long value) {
         this.value = value;
      }

      @Override
      public String toString() {
         return "<= " + this.value;
      }

      public Schema.DateConstraintV1 serialize() {
         return Schema.DateConstraintV1.newBuilder()
                 .setBefore(this.value).build();
      }

      static public Either<Error.FormatError, DateConstraint> deserializeV0(Schema.DateConstraintV0 i) {
         if(!i.hasBefore()) {
            return Left(new Error.FormatError.DeserializationError("invalid Date constraint"));
         } else {
            return Right(new Before(i.getBefore()));
         }
      }

      static public Either<Error.FormatError, DateConstraint> deserializeV1(Schema.DateConstraintV1 i) {
         if(!i.hasBefore()) {
            return Left(new Error.FormatError.DeserializationError("invalid Date constraint"));
         } else {
            return Right(new Before(i.getBefore()));
         }
      }
   }

   public static final class After extends DateConstraint implements Serializable {
      private final long value;

      public boolean check(final long value) {
         return this.value <= value;
      }

      public After(final long value) {
         this.value = value;
      }

      @Override
      public String toString() {
         return ">= " + this.value;
      }

      public Schema.DateConstraintV1 serialize() {
         return Schema.DateConstraintV1.newBuilder()
                 .setAfter(this.value).build();
      }

      static public Either<Error.FormatError, DateConstraint> deserializeV0(Schema.DateConstraintV0 i) {
         if(!i.hasAfter()) {
            return Left(new Error.FormatError.DeserializationError("invalid Date constraint"));
         } else {
            return Right(new After(i.getAfter()));
         }
      }

      static public Either<Error.FormatError, DateConstraint> deserializeV1(Schema.DateConstraintV1 i) {
         if(!i.hasAfter()) {
            return Left(new Error.FormatError.DeserializationError("invalid Date constraint"));
         } else {
            return Right(new After(i.getAfter()));
         }
      }
   }
}
