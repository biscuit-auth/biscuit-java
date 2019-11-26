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
   public abstract Schema.IntConstraint serialize();

   static public Either<Error.FormatError, IntConstraint> deserialize_enum(Schema.IntConstraint c) {
      if(c.getKind() == Schema.IntConstraint.Kind.LOWER) {
         return Lower.deserialize(c);
      } else if(c.getKind() == Schema.IntConstraint.Kind.LARGER) {
         return Greater.deserialize(c);
      } else if(c.getKind() == Schema.IntConstraint.Kind.LOWER_OR_EQUAL) {
         return LowerOrEqual.deserialize(c);
      } else if(c.getKind() == Schema.IntConstraint.Kind.LARGER_OR_EQUAL) {
         return GreaterOrEqual.deserialize(c);
      } else if(c.getKind() == Schema.IntConstraint.Kind.EQUAL) {
         return Equal.deserialize(c);
      } else if(c.getKind() == Schema.IntConstraint.Kind.IN) {
         return InSet.deserialize(c);
      } else if(c.getKind() == Schema.IntConstraint.Kind.NOT_IN) {
         return NotInSet.deserialize(c);
      } else {
         return Left(new Error().new FormatError().new DeserializationError("invalid int constraint kind"));
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

      public Schema.IntConstraint serialize() {
         return Schema.IntConstraint.newBuilder()
                 .setKind(Schema.IntConstraint.Kind.EQUAL)
                 .setEqual(this.value).build();
      }

      static public Either<Error.FormatError, IntConstraint> deserialize(Schema.IntConstraint i) {
         if(!i.hasEqual()) {
            return Left(new Error().new FormatError().new DeserializationError("invalid Int constraint"));
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

      public Schema.IntConstraint serialize() {
         return Schema.IntConstraint.newBuilder()
                 .setKind(Schema.IntConstraint.Kind.LARGER)
                 .setLarger(this.value).build();
      }

      static public Either<Error.FormatError, IntConstraint> deserialize(Schema.IntConstraint i) {
         if(!i.hasLarger()) {
            return Left(new Error().new FormatError().new DeserializationError("invalid Int constraint"));
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

      public Schema.IntConstraint serialize() {
         return Schema.IntConstraint.newBuilder()
                 .setKind(Schema.IntConstraint.Kind.LARGER_OR_EQUAL)
                 .setLargerOrEqual(this.value).build();
      }

      static public Either<Error.FormatError, IntConstraint> deserialize(Schema.IntConstraint i) {
         if(!i.hasLargerOrEqual()) {
            return Left(new Error().new FormatError().new DeserializationError("invalid Int constraint"));
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

      public Schema.IntConstraint serialize() {
         return Schema.IntConstraint.newBuilder()
                 .setKind(Schema.IntConstraint.Kind.LOWER)
                 .setLower(this.value).build();
      }

      static public Either<Error.FormatError, IntConstraint> deserialize(Schema.IntConstraint i) {
         if(!i.hasLower()) {
            return Left(new Error().new FormatError().new DeserializationError("invalid Int constraint"));
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

      public Schema.IntConstraint serialize() {
         return Schema.IntConstraint.newBuilder()
                 .setKind(Schema.IntConstraint.Kind.LOWER_OR_EQUAL)
                 .setLowerOrEqual(this.value).build();
      }

      static public Either<Error.FormatError, IntConstraint> deserialize(Schema.IntConstraint i) {
         if(!i.hasLowerOrEqual()) {
            return Left(new Error().new FormatError().new DeserializationError("invalid Int constraint"));
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

      public Schema.IntConstraint serialize() {
         Schema.IntConstraint.Builder b = Schema.IntConstraint.newBuilder()
                 .setKind(Schema.IntConstraint.Kind.IN);
         for (Long l: this.value) {
            b.addInSet(l);
         }
         return b.build();
      }

      static public Either<Error.FormatError, IntConstraint> deserialize(Schema.IntConstraint i) {
         Set<Long> values = new HashSet<>();
         for (long l: i.getInSetList()) {
            values.add(l);
         }
         if(values.isEmpty()) {
            return Left(new Error().new FormatError().new DeserializationError("invalid Int constraint"));
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

      public Schema.IntConstraint serialize() {
         Schema.IntConstraint.Builder b = Schema.IntConstraint.newBuilder()
                 .setKind(Schema.IntConstraint.Kind.NOT_IN);
         for (Long l: this.value) {
            b.addNotInSet(l);
         }
         return b.build();
      }

      static public Either<Error.FormatError, IntConstraint> deserialize(Schema.IntConstraint i) {
         Set<Long> values = new HashSet<>();
         for (long l: i.getNotInSetList()) {
            values.add(l);
         }
         if(values.isEmpty()) {
            return Left(new Error().new FormatError().new DeserializationError("invalid Int constraint"));
         } else {
            return Right(new NotInSet(values));
         }
      }
   }
}
