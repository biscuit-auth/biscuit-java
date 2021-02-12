package com.clevercloud.biscuit.datalog.constraints;

import biscuit.format.schema.Schema;

import java.io.Serializable;
import com.clevercloud.biscuit.error.Error;
import io.vavr.control.Either;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

public abstract class ConstraintKind implements Serializable {
   public abstract Schema.ConstraintV1 serialize(long id);
   static public Either<Error.FormatError, ConstraintKind> deserialize_enumV0(Schema.ConstraintV0 c) {
      if(c.getKind() == Schema.ConstraintV0.Kind.INT) {
         return Int.deserializeV0(c);
      } else if(c.getKind() == Schema.ConstraintV0.Kind.STRING) {
         return Str.deserializeV0(c);
      } else if(c.getKind() == Schema.ConstraintV0.Kind.DATE) {
         return Date.deserializeV0(c);
      } else if(c.getKind() == Schema.ConstraintV0.Kind.SYMBOL) {
         return Symbol.deserializeV0(c);
      } else {
         return Left(new Error.FormatError.DeserializationError("invalid constraint kind"));
      }
   }

   static public Either<Error.FormatError, ConstraintKind> deserialize_enumV1(Schema.ConstraintV1 c) {
      if(c.getKind() == Schema.ConstraintV1.Kind.INT) {
         return Int.deserializeV1(c);
      } else if(c.getKind() == Schema.ConstraintV1.Kind.STRING) {
         return Str.deserializeV1(c);
      } else if(c.getKind() == Schema.ConstraintV1.Kind.DATE) {
         return Date.deserializeV1(c);
      } else if(c.getKind() == Schema.ConstraintV1.Kind.SYMBOL) {
         return Symbol.deserializeV1(c);
      } else {
         return Left(new Error.FormatError.DeserializationError("invalid constraint kind"));
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

      public Schema.ConstraintV1 serialize(long id) {
         return Schema.ConstraintV1.newBuilder()
                 .setId((int) id)
                 .setKind(Schema.ConstraintV1.Kind.INT)
                 .setInt(this.constraint.serialize())
                 .build();
      }

      static public Either<Error.FormatError, ConstraintKind> deserializeV0(Schema.ConstraintV0 c) {
         long id = c.getId();

         if(!c.hasInt()) {
            return Left(new Error.FormatError.DeserializationError("invalid Int constraint"));
         } else {
            Either<Error.FormatError, IntConstraint> res = IntConstraint.deserialize_enumV0(c.getInt());
            if (res.isLeft()) {
               Error.FormatError e = res.getLeft();
               return Left(e);
            } else {
               return Right(new Int(res.get()));
            }
         }
      }

      static public Either<Error.FormatError, ConstraintKind> deserializeV1(Schema.ConstraintV1 c) {
         long id = c.getId();

         if(!c.hasInt()) {
            return Left(new Error.FormatError.DeserializationError("invalid Int constraint"));
         } else {
            Either<Error.FormatError, IntConstraint> res = IntConstraint.deserialize_enumV1(c.getInt());
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

      public Schema.ConstraintV1 serialize(long id) {
         return Schema.ConstraintV1.newBuilder()
                 .setId((int) id)
                 .setKind(Schema.ConstraintV1.Kind.STRING)
                 .setStr(this.constraint.serialize())
                 .build();
      }

      static public Either<Error.FormatError, ConstraintKind> deserializeV0(Schema.ConstraintV0 c) {
         long id = c.getId();

         if (!c.hasStr()) {
            return Left(new Error.FormatError.DeserializationError("invalid Str constraint"));
         } else {
            Either<Error.FormatError, StrConstraint> res = StrConstraint.deserialize_enumV0(c.getStr());
            if (res.isLeft()) {
               Error.FormatError e = res.getLeft();
               return Left(e);
            } else {
               return Right(new Str(res.get()));
            }
         }
      }

      static public Either<Error.FormatError, ConstraintKind> deserializeV1(Schema.ConstraintV1 c) {
         long id = c.getId();

         if (!c.hasStr()) {
            return Left(new Error.FormatError.DeserializationError("invalid Str constraint"));
         } else {
            Either<Error.FormatError, StrConstraint> res = StrConstraint.deserialize_enumV1(c.getStr());
            if (res.isLeft()) {
               Error.FormatError e = res.getLeft();
               return Left(e);
            } else {
               return Right(new Str(res.get()));
            }
         }
      }
   }

   public static final class Bytes extends ConstraintKind implements Serializable {
      private final BytesConstraint constraint;

      public boolean check(final byte[] value) {
         return this.constraint.check(value);
      }

      public Bytes(final BytesConstraint constraint) {
         this.constraint = constraint;
      }

      @Override
      public String toString() {
         return this.constraint.toString();
      }

      public Schema.ConstraintV1 serialize(long id) {
         return Schema.ConstraintV1.newBuilder()
                 .setId((int) id)
                 .setKind(Schema.ConstraintV1.Kind.BYTES)
                 .setBytes(this.constraint.serialize())
                 .build();
      }

      static public Either<Error.FormatError, ConstraintKind> deserializeV0(Schema.ConstraintV0 c) {
         long id = c.getId();

         if (!c.hasStr()) {
            return Left(new Error.FormatError.DeserializationError("invalid Bytes constraint"));
         } else {
            Either<Error.FormatError, BytesConstraint> res = BytesConstraint.deserialize_enumV0(c.getBytes());
            if (res.isLeft()) {
               Error.FormatError e = res.getLeft();
               return Left(e);
            } else {
               return Right(new Bytes(res.get()));
            }
         }
      }

      static public Either<Error.FormatError, ConstraintKind> deserializeV1(Schema.ConstraintV1 c) {
         long id = c.getId();

         if (!c.hasStr()) {
            return Left(new Error.FormatError.DeserializationError("invalid Bytes constraint"));
         } else {
            Either<Error.FormatError, BytesConstraint> res = BytesConstraint.deserialize_enumV1(c.getBytes());
            if (res.isLeft()) {
               Error.FormatError e = res.getLeft();
               return Left(e);
            } else {
               return Right(new Bytes(res.get()));
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

      public Schema.ConstraintV1 serialize(long id) {
         return Schema.ConstraintV1.newBuilder()
                 .setId((int) id)
                 .setKind(Schema.ConstraintV1.Kind.DATE)
                 .setDate(this.constraint.serialize())
                 .build();
      }

      static public Either<Error.FormatError, ConstraintKind> deserializeV0(Schema.ConstraintV0 c) {
         long id = c.getId();

         if (!c.hasDate()) {
            return Left(new Error.FormatError.DeserializationError("invalid Date constraint"));
         } else {
            Either<Error.FormatError, DateConstraint> res = DateConstraint.deserialize_enumV0(c.getDate());
            if (res.isLeft()) {
               Error.FormatError e = res.getLeft();
               return Left(e);
            } else {
               return Right(new Date(res.get()));
            }
         }
      }

      static public Either<Error.FormatError, ConstraintKind> deserializeV1(Schema.ConstraintV1 c) {
         long id = c.getId();

         if (!c.hasDate()) {
            return Left(new Error.FormatError.DeserializationError("invalid Date constraint"));
         } else {
            Either<Error.FormatError, DateConstraint> res = DateConstraint.deserialize_enumV1(c.getDate());
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
      public final SymbolConstraint constraint;

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

      public Schema.ConstraintV1 serialize(long id) {
         return Schema.ConstraintV1.newBuilder()
                 .setId((int) id)
                 .setKind(Schema.ConstraintV1.Kind.SYMBOL)
                 .setSymbol(this.constraint.serialize())
                 .build();
      }

      static public Either<Error.FormatError, ConstraintKind> deserializeV0(Schema.ConstraintV0 c) {
         long id = c.getId();

         if (!c.hasSymbol()) {
            return Left(new Error.FormatError.DeserializationError("invalid Symbol constraint"));
         } else {
            Either<Error.FormatError, SymbolConstraint> res = SymbolConstraint.deserialize_enumV0(c.getSymbol());
            if (res.isLeft()) {
               Error.FormatError e = res.getLeft();
               return Left(e);
            } else {
               return Right(new Symbol(res.get()));
            }
         }
      }

      static public Either<Error.FormatError, ConstraintKind> deserializeV1(Schema.ConstraintV1 c) {
         long id = c.getId();

         if (!c.hasSymbol()) {
            return Left(new Error.FormatError.DeserializationError("invalid Symbol constraint"));
         } else {
            Either<Error.FormatError, SymbolConstraint> res = SymbolConstraint.deserialize_enumV1(c.getSymbol());
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
