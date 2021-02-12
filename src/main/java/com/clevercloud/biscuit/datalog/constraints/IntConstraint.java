package com.clevercloud.biscuit.datalog.constraints;

import biscuit.format.schema.Schema;
import com.clevercloud.biscuit.error.Error;
import io.vavr.control.Either;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

public abstract class IntConstraint implements Serializable {
   public abstract boolean check(final long value);
   public abstract Schema.IntConstraintV1 serialize();

   static public Either<Error.FormatError, IntConstraint> deserialize_enumV0(Schema.IntConstraintV0 c) {
      if(c.getKind() == Schema.IntConstraintV0.Kind.LOWER) {
         return Lower.deserializeV0(c);
      } else if(c.getKind() == Schema.IntConstraintV0.Kind.LARGER) {
         return Greater.deserializeV0(c);
      } else if(c.getKind() == Schema.IntConstraintV0.Kind.LOWER_OR_EQUAL) {
         return LowerOrEqual.deserializeV0(c);
      } else if(c.getKind() == Schema.IntConstraintV0.Kind.LARGER_OR_EQUAL) {
         return GreaterOrEqual.deserializeV0(c);
      } else if(c.getKind() == Schema.IntConstraintV0.Kind.EQUAL) {
         return Equal.deserializeV0(c);
      } else if(c.getKind() == Schema.IntConstraintV0.Kind.IN) {
         return InSet.deserializeV0(c);
      } else if(c.getKind() == Schema.IntConstraintV0.Kind.NOT_IN) {
         return NotInSet.deserializeV0(c);
      } else {
         return Left(new Error.FormatError.DeserializationError("invalid int constraint kind"));
      }
   }

   static public Either<Error.FormatError, IntConstraint> deserialize_enumV1(Schema.IntConstraintV1 c) {
      if(c.getKind() == Schema.IntConstraintV1.Kind.LOWER) {
         return Lower.deserializeV1(c);
      } else if(c.getKind() == Schema.IntConstraintV1.Kind.LARGER) {
         return Greater.deserializeV1(c);
      } else if(c.getKind() == Schema.IntConstraintV1.Kind.LOWER_OR_EQUAL) {
         return LowerOrEqual.deserializeV1(c);
      } else if(c.getKind() == Schema.IntConstraintV1.Kind.LARGER_OR_EQUAL) {
         return GreaterOrEqual.deserializeV1(c);
      } else if(c.getKind() == Schema.IntConstraintV1.Kind.EQUAL) {
         return Equal.deserializeV1(c);
      } else if(c.getKind() == Schema.IntConstraintV1.Kind.IN) {
         return InSet.deserializeV1(c);
      } else if(c.getKind() == Schema.IntConstraintV1.Kind.NOT_IN) {
         return NotInSet.deserializeV1(c);
      } else {
         return Left(new Error.FormatError.DeserializationError("invalid int constraint kind"));
      }
   }

   public static final class Equal extends IntConstraint implements Serializable {
      private final long value;

      public boolean check(final long value) {
         return this.value == value;
      }

      public Equal(final long value) {
         this.value = value;
      }

      @Override
      public String toString() {
         return "== " + this.value;
      }

      public Schema.IntConstraintV1 serialize() {
         return Schema.IntConstraintV1.newBuilder()
                 .setKind(Schema.IntConstraintV1.Kind.EQUAL)
                 .setEqual(this.value).build();
      }

      static public Either<Error.FormatError, IntConstraint> deserializeV0(Schema.IntConstraintV0 i) {
         if(!i.hasEqual()) {
            return Left(new Error.FormatError.DeserializationError("invalid Int constraint"));
         } else {
            return Right(new Equal(i.getEqual()));
         }
      }

      static public Either<Error.FormatError, IntConstraint> deserializeV1(Schema.IntConstraintV1 i) {
         if(!i.hasEqual()) {
            return Left(new Error.FormatError.DeserializationError("invalid Int constraint"));
         } else {
            return Right(new Equal(i.getEqual()));
         }
      }
   }

   public static final class Greater extends IntConstraint implements Serializable {
      private final long value;

      public boolean check(final long value) {
         return this.value < value;
      }

      public Greater(final long value) {
         this.value = value;
      }

      @Override
      public String toString() {
         return "> " + this.value;
      }

      public Schema.IntConstraintV1 serialize() {
         return Schema.IntConstraintV1.newBuilder()
                 .setKind(Schema.IntConstraintV1.Kind.LARGER)
                 .setLarger(this.value).build();
      }

      static public Either<Error.FormatError, IntConstraint> deserializeV0(Schema.IntConstraintV0 i) {
         if(!i.hasLarger()) {
            return Left(new Error.FormatError.DeserializationError("invalid Int constraint"));
         } else {
            return Right(new Greater(i.getLarger()));
         }
      }

      static public Either<Error.FormatError, IntConstraint> deserializeV1(Schema.IntConstraintV1 i) {
         if(!i.hasLarger()) {
            return Left(new Error.FormatError.DeserializationError("invalid Int constraint"));
         } else {
            return Right(new Greater(i.getLarger()));
         }
      }
   }

   public static final class GreaterOrEqual extends IntConstraint implements Serializable {
      private final long value;

      public boolean check(final long value) {
         return this.value <= value;
      }

      public GreaterOrEqual(final long value) {
         this.value = value;
      }

      @Override
      public String toString() {
         return ">= " + this.value;
      }

      public Schema.IntConstraintV1 serialize() {
         return Schema.IntConstraintV1.newBuilder()
                 .setKind(Schema.IntConstraintV1.Kind.LARGER_OR_EQUAL)
                 .setLargerOrEqual(this.value).build();
      }

      static public Either<Error.FormatError, IntConstraint> deserializeV0(Schema.IntConstraintV0 i) {
         if(!i.hasLargerOrEqual()) {
            return Left(new Error.FormatError.DeserializationError("invalid Int constraint"));
         } else {
            return Right(new GreaterOrEqual(i.getLargerOrEqual()));
         }
      }

      static public Either<Error.FormatError, IntConstraint> deserializeV1(Schema.IntConstraintV1 i) {
         if(!i.hasLargerOrEqual()) {
            return Left(new Error.FormatError.DeserializationError("invalid Int constraint"));
         } else {
            return Right(new GreaterOrEqual(i.getLargerOrEqual()));
         }
      }
   }

   public static final class Lower extends IntConstraint implements Serializable {
      private final long value;

      public boolean check(final long value) {
         return this.value > value;
      }

      public Lower(final long value) {
         this.value = value;
      }

      @Override
      public String toString() {
         return "< " + this.value;
      }

      public Schema.IntConstraintV1 serialize() {
         return Schema.IntConstraintV1.newBuilder()
                 .setKind(Schema.IntConstraintV1.Kind.LOWER)
                 .setLower(this.value).build();
      }

      static public Either<Error.FormatError, IntConstraint> deserializeV0(Schema.IntConstraintV0 i) {
         if(!i.hasLower()) {
            return Left(new Error.FormatError.DeserializationError("invalid Int constraint"));
         } else {
            return Right(new Lower(i.getLower()));
         }
      }

      static public Either<Error.FormatError, IntConstraint> deserializeV1(Schema.IntConstraintV1 i) {
         if(!i.hasLower()) {
            return Left(new Error.FormatError.DeserializationError("invalid Int constraint"));
         } else {
            return Right(new Lower(i.getLower()));
         }
      }
   }

   public static final class LowerOrEqual extends IntConstraint implements Serializable {
      private final long value;

      public boolean check(final long value) {
         return this.value >= value;
      }

      public LowerOrEqual(final long value) {
         this.value = value;
      }

      @Override
      public String toString() {
         return "<= " + this.value;
      }

      public Schema.IntConstraintV1 serialize() {
         return Schema.IntConstraintV1.newBuilder()
                 .setKind(Schema.IntConstraintV1.Kind.LOWER_OR_EQUAL)
                 .setLowerOrEqual(this.value).build();
      }

      static public Either<Error.FormatError, IntConstraint> deserializeV0(Schema.IntConstraintV0 i) {
         if(!i.hasLowerOrEqual()) {
            return Left(new Error.FormatError.DeserializationError("invalid Int constraint"));
         } else {
            return Right(new LowerOrEqual(i.getLowerOrEqual()));
         }
      }

      static public Either<Error.FormatError, IntConstraint> deserializeV1(Schema.IntConstraintV1 i) {
         if(!i.hasLowerOrEqual()) {
            return Left(new Error.FormatError.DeserializationError("invalid Int constraint"));
         } else {
            return Right(new LowerOrEqual(i.getLowerOrEqual()));
         }
      }
   }

   public static final class InSet extends IntConstraint implements Serializable {
      private final Set<Long> value;

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

      public Schema.IntConstraintV1 serialize() {
         Schema.IntConstraintV1.Builder b = Schema.IntConstraintV1.newBuilder()
                 .setKind(Schema.IntConstraintV1.Kind.IN);
         for (Long l: this.value) {
            b.addInSet(l);
         }
         return b.build();
      }

      static public Either<Error.FormatError, IntConstraint> deserializeV0(Schema.IntConstraintV0 i) {
         Set<Long> values = new HashSet<>();
         for (long l: i.getInSetList()) {
            values.add(l);
         }
         if(values.isEmpty()) {
            return Left(new Error.FormatError.DeserializationError("invalid Int constraint"));
         } else {
            return Right(new InSet(values));
         }
      }

      static public Either<Error.FormatError, IntConstraint> deserializeV1(Schema.IntConstraintV1 i) {
         Set<Long> values = new HashSet<>();
         for (long l: i.getInSetList()) {
            values.add(l);
         }
         if(values.isEmpty()) {
            return Left(new Error.FormatError.DeserializationError("invalid Int constraint"));
         } else {
            return Right(new InSet(values));
         }
      }
   }

   public static final class NotInSet extends IntConstraint implements Serializable {
      private final Set<Long> value;

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

      public Schema.IntConstraintV1 serialize() {
         Schema.IntConstraintV1.Builder b = Schema.IntConstraintV1.newBuilder()
                 .setKind(Schema.IntConstraintV1.Kind.NOT_IN);
         for (Long l: this.value) {
            b.addNotInSet(l);
         }
         return b.build();
      }

      static public Either<Error.FormatError, IntConstraint> deserializeV0(Schema.IntConstraintV0 i) {
         Set<Long> values = new HashSet<>();
         for (long l: i.getNotInSetList()) {
            values.add(l);
         }
         if(values.isEmpty()) {
            return Left(new Error.FormatError.DeserializationError("invalid Int constraint"));
         } else {
            return Right(new NotInSet(values));
         }
      }

      static public Either<Error.FormatError, IntConstraint> deserializeV1(Schema.IntConstraintV1 i) {
         Set<Long> values = new HashSet<>();
         for (long l: i.getNotInSetList()) {
            values.add(l);
         }
         if(values.isEmpty()) {
            return Left(new Error.FormatError.DeserializationError("invalid Int constraint"));
         } else {
            return Right(new NotInSet(values));
         }
      }
   }
}
