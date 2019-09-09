package com.clevercloud.biscuit.datalog.constraints;

import biscuit.format.schema.Schema;

import java.io.Serializable;
import com.clevercloud.biscuit.error.Error;
import io.vavr.control.Either;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

public abstract class ConstraintKind implements Serializable {
   public abstract Schema.Constraint serialize(long id);
   static public Either<Error.FormatError, ConstraintKind> deserialize_enum(Schema.Constraint c) {
      if(c.getKind() == Schema.Constraint.Kind.INT) {
         return Int.deserialize(c);
      } else if(c.getKind() == Schema.Constraint.Kind.STRING) {
         return Str.deserialize(c);
      } else if(c.getKind() == Schema.Constraint.Kind.DATE) {
         return Date.deserialize(c);
      } else if(c.getKind() == Schema.Constraint.Kind.SYMBOL) {
         return Symbol.deserialize(c);
      } else {
         return Left(new Error().new FormatError().new DeserializationError("invalid constraint kind"));
      }
   }


   public static final class Int extends ConstraintKind implements Serializable {
      private final IntConstraint constraint;

      public boolean check(final long value) {
         return this.constraint.check(value);
      }

      public Int(final IntConstraint constraint) {
         this.constraint = constraint;
      }

      @Override
      public String toString() {
         return this.constraint.toString();
      }

      public Schema.Constraint serialize(long id) {
         return Schema.Constraint.newBuilder()
                 .setId((int) id)
                 .setKind(Schema.Constraint.Kind.INT)
                 .setInt(this.constraint.serialize())
                 .build();
      }

      static public Either<Error.FormatError, ConstraintKind> deserialize(Schema.Constraint c) {
         long id = c.getId();

         if(!c.hasInt()) {
            return Left(new Error().new FormatError().new DeserializationError("invalid Int constraint"));
         } else {
            Either<Error.FormatError, IntConstraint> res = IntConstraint.deserialize_enum(c.getInt());
            if (res.isLeft()) {
               Error.FormatError e = res.getLeft();
               return Left(e);
            } else {
               return Right(new Int(res.get()));
            }
         }
      }
   }

   public static final class Str extends ConstraintKind implements Serializable {
      private final StrConstraint constraint;

      public boolean check(final String value) {
         return this.constraint.check(value);
      }

      public Str(final StrConstraint constraint) {
         this.constraint = constraint;
      }

      @Override
      public String toString() {
         return this.constraint.toString();
      }

      public Schema.Constraint serialize(long id) {
         return Schema.Constraint.newBuilder()
                 .setId((int) id)
                 .setKind(Schema.Constraint.Kind.STRING)
                 .setStr(this.constraint.serialize())
                 .build();
      }

      static public Either<Error.FormatError, ConstraintKind> deserialize(Schema.Constraint c) {
         long id = c.getId();

         if (!c.hasStr()) {
            return Left(new Error().new FormatError().new DeserializationError("invalid Str constraint"));
         } else {
            Either<Error.FormatError, StrConstraint> res = StrConstraint.deserialize_enum(c.getStr());
            if (res.isLeft()) {
               Error.FormatError e = res.getLeft();
               return Left(e);
            } else {
               return Right(new Str(res.get()));
            }
         }
      }
   }

   public static final class Date extends ConstraintKind implements Serializable {
      private final DateConstraint constraint;

      public boolean check(final long value) {
         return this.constraint.check(value);
      }

      public Date(final DateConstraint constraint) {
         this.constraint = constraint;
      }

      @Override
      public String toString() {
         return this.constraint.toString();
      }

      public Schema.Constraint serialize(long id) {
         return Schema.Constraint.newBuilder()
                 .setId((int) id)
                 .setKind(Schema.Constraint.Kind.DATE)
                 .setDate(this.constraint.serialize())
                 .build();
      }

      static public Either<Error.FormatError, ConstraintKind> deserialize(Schema.Constraint c) {
         long id = c.getId();

         if (!c.hasDate()) {
            return Left(new Error().new FormatError().new DeserializationError("invalid Date constraint"));
         } else {
            Either<Error.FormatError, DateConstraint> res = DateConstraint.deserialize_enum(c.getDate());
            if (res.isLeft()) {
               Error.FormatError e = res.getLeft();
               return Left(e);
            } else {
               return Right(new Date(res.get()));
            }
         }
      }
   }

   public static final class Symbol extends ConstraintKind implements Serializable {
      private final SymbolConstraint constraint;

      public boolean check(final long value) {
         return this.constraint.check(value);
      }

      public Symbol(final SymbolConstraint constraint) {
         this.constraint = constraint;
      }

      @Override
      public String toString() {
         return this.constraint.toString();
      }

      public Schema.Constraint serialize(long id) {
         return Schema.Constraint.newBuilder()
                 .setId((int) id)
                 .setKind(Schema.Constraint.Kind.SYMBOL)
                 .setSymbol(this.constraint.serialize())
                 .build();
      }

      static public Either<Error.FormatError, ConstraintKind> deserialize(Schema.Constraint c) {
         long id = c.getId();

         if (!c.hasSymbol()) {
            return Left(new Error().new FormatError().new DeserializationError("invalid Symbol constraint"));
         } else {
            Either<Error.FormatError, SymbolConstraint> res = SymbolConstraint.deserialize_enum(c.getSymbol());
            if (res.isLeft()) {
               Error.FormatError e = res.getLeft();
               return Left(e);
            } else {
               return Right(new Symbol(res.get()));
            }
         }
      }
   }
}
