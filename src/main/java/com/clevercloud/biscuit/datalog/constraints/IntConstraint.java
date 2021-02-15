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
         return LessThan.deserializeV0(c);
      } else if(c.getKind() == Schema.IntConstraintV0.Kind.LARGER) {
         return GreaterThan.deserializeV0(c);
      } else if(c.getKind() == Schema.IntConstraintV0.Kind.LOWER_OR_EQUAL) {
         return LessOrEqual.deserializeV0(c);
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
      if(c.hasLessThan()) {
         return LessThan.deserializeV1(c);
      } else if(c.hasGreaterThan()) {
         return GreaterThan.deserializeV1(c);
      } else if(c.hasLessOrEqual()) {
         return LessOrEqual.deserializeV1(c);
      } else if(c.hasGreaterOrEqual()) {
         return GreaterOrEqual.deserializeV1(c);
      } else if(c.hasEqual()) {
         return Equal.deserializeV1(c);
      } else if(c.hasInSet()) {
         return InSet.deserializeV1(c);
      } else if(c.hasNotInSet()) {
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

   public static final class GreaterThan extends IntConstraint implements Serializable {
      private final long value;

      public boolean check(final long value) {
         return this.value < value;
      }

      public GreaterThan(final long value) {
         this.value = value;
      }

      @Override
      public String toString() {
         return "> " + this.value;
      }

      public Schema.IntConstraintV1 serialize() {
         return Schema.IntConstraintV1.newBuilder()
                 .setGreaterThan(this.value).build();
      }

      static public Either<Error.FormatError, IntConstraint> deserializeV0(Schema.IntConstraintV0 i) {
         if(!i.hasLarger()) {
            return Left(new Error.FormatError.DeserializationError("invalid Int constraint"));
         } else {
            return Right(new GreaterThan(i.getLarger()));
         }
      }

      static public Either<Error.FormatError, IntConstraint> deserializeV1(Schema.IntConstraintV1 i) {
         if(!i.hasGreaterThan()) {
            return Left(new Error.FormatError.DeserializationError("invalid Int constraint"));
         } else {
            return Right(new GreaterThan(i.getGreaterThan()));
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
                 .setGreaterOrEqual(this.value).build();
      }

      static public Either<Error.FormatError, IntConstraint> deserializeV0(Schema.IntConstraintV0 i) {
         if(!i.hasLargerOrEqual()) {
            return Left(new Error.FormatError.DeserializationError("invalid Int constraint"));
         } else {
            return Right(new GreaterOrEqual(i.getLargerOrEqual()));
         }
      }

      static public Either<Error.FormatError, IntConstraint> deserializeV1(Schema.IntConstraintV1 i) {
         if(!i.hasGreaterOrEqual()) {
            return Left(new Error.FormatError.DeserializationError("invalid Int constraint"));
         } else {
            return Right(new GreaterOrEqual(i.getGreaterOrEqual()));
         }
      }
   }

   public static final class LessThan extends IntConstraint implements Serializable {
      private final long value;

      public boolean check(final long value) {
         return this.value > value;
      }

      public LessThan(final long value) {
         this.value = value;
      }

      @Override
      public String toString() {
         return "< " + this.value;
      }

      public Schema.IntConstraintV1 serialize() {
         return Schema.IntConstraintV1.newBuilder()
                 .setLessOrEqual(this.value).build();
      }

      static public Either<Error.FormatError, IntConstraint> deserializeV0(Schema.IntConstraintV0 i) {
         if(!i.hasLower()) {
            return Left(new Error.FormatError.DeserializationError("invalid Int constraint"));
         } else {
            return Right(new LessThan(i.getLower()));
         }
      }

      static public Either<Error.FormatError, IntConstraint> deserializeV1(Schema.IntConstraintV1 i) {
         if(!i.hasLessThan()) {
            return Left(new Error.FormatError.DeserializationError("invalid Int constraint"));
         } else {
            return Right(new LessThan(i.getLessThan()));
         }
      }
   }

   public static final class LessOrEqual extends IntConstraint implements Serializable {
      private final long value;

      public boolean check(final long value) {
         return this.value >= value;
      }

      public LessOrEqual(final long value) {
         this.value = value;
      }

      @Override
      public String toString() {
         return "<= " + this.value;
      }

      public Schema.IntConstraintV1 serialize() {
         return Schema.IntConstraintV1.newBuilder()
                 .setLessOrEqual(this.value).build();
      }

      static public Either<Error.FormatError, IntConstraint> deserializeV0(Schema.IntConstraintV0 i) {
         if(!i.hasLowerOrEqual()) {
            return Left(new Error.FormatError.DeserializationError("invalid Int constraint"));
         } else {
            return Right(new LessOrEqual(i.getLowerOrEqual()));
         }
      }

      static public Either<Error.FormatError, IntConstraint> deserializeV1(Schema.IntConstraintV1 i) {
         if(!i.hasLessOrEqual()) {
            return Left(new Error.FormatError.DeserializationError("invalid Int constraint"));
         } else {
            return Right(new LessOrEqual(i.getLessOrEqual()));
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
         Schema.IntConstraintV1.Builder b = Schema.IntConstraintV1.newBuilder();
         Schema.IntSet.Builder s = Schema.IntSet.newBuilder();

         for (Long l: this.value) {
            s.addSet(l);
         }
         b.setInSet(s);

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
         Schema.IntSet s = i.getInSet();
         for (long l: s.getSetList()) {
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
         Schema.IntConstraintV1.Builder b = Schema.IntConstraintV1.newBuilder();
         Schema.IntSet.Builder s = Schema.IntSet.newBuilder();

         for (Long l: this.value) {
            s.addSet(l);
         }
         b.setNotInSet(s);

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
         Schema.IntSet s = i.getNotInSet();
         for (long l: s.getSetList()) {
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
