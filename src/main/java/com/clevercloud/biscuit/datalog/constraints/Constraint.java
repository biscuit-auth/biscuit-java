package com.clevercloud.biscuit.datalog.constraints;

import biscuit.format.schema.Schema;
import com.clevercloud.biscuit.datalog.ID;
import com.clevercloud.biscuit.datalog.expressions.Expression;
import com.clevercloud.biscuit.datalog.expressions.Op;
import com.clevercloud.biscuit.error.Error;
import com.google.protobuf.ByteString;
import io.vavr.control.Either;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

public final class Constraint implements Serializable {
   public final long id;
   public final ConstraintKind kind;

   public Constraint(long id, ConstraintKind kind) {
      this.id = id;
      this.kind = kind;
   }

   public boolean check(final long name, final ID id) {
      if (name != this.id) {
         return true;
      }

      if (id instanceof ID.Variable) {
         assert "should not check constraint on a variable" == null;
         return false;
      } else if (id instanceof ID.Integer && this.kind instanceof ConstraintKind.Int) {
         return ((ConstraintKind.Int) this.kind).check(((ID.Integer) id).value());
      } else if (id instanceof ID.Str && this.kind instanceof ConstraintKind.Str) {
         return ((ConstraintKind.Str) this.kind).check(((ID.Str) id).value());
      } else if (id instanceof ID.Date && this.kind instanceof ConstraintKind.Date) {
         return ((ConstraintKind.Date) this.kind).check(((ID.Date) id).value());
      } else if (id instanceof ID.Symbol && this.kind instanceof ConstraintKind.Symbol) {
         return ((ConstraintKind.Symbol) this.kind).check(((ID.Symbol) id).value());
      } else {
         return false;
      }
   }

   @Override
   public String toString() {
      return "$" + this.id + " " + this.kind.toString();
   }

   public Schema.ConstraintV1 serialize() {
      return this.kind.serialize(this.id);
   }

   static public Either<Error.FormatError, Expression> deserializeV0(Schema.ConstraintV0 c) {
      ArrayList<Op> ops = new ArrayList<Op>();

      long id = c.getId();
      ops.add(new Op.Value(new ID.Variable(id)));

      switch(c.getKind()) {
         case INT:
            if (c.hasInt()) {
               Schema.IntConstraintV0 ic = c.getInt();

               switch (ic.getKind()) {
                  case LOWER:
                     ops.add(new Op.Value(new ID.Integer(ic.getLower())));
                     ops.add(new Op.Binary(Op.BinaryOp.LessThan));
                     return Right(new Expression(ops));
                  case LARGER:
                     ops.add(new Op.Value(new ID.Integer(ic.getLarger())));
                     ops.add(new Op.Binary(Op.BinaryOp.GreaterThan));
                     return Right(new Expression(ops));
                  case LOWER_OR_EQUAL:
                     ops.add(new Op.Value(new ID.Integer(ic.getLowerOrEqual())));
                     ops.add(new Op.Binary(Op.BinaryOp.LessOrEqual));
                     return Right(new Expression(ops));
                  case LARGER_OR_EQUAL:
                     ops.add(new Op.Value(new ID.Integer(ic.getLargerOrEqual())));
                     ops.add(new Op.Binary(Op.BinaryOp.GreaterOrEqual));
                     return Right(new Expression(ops));
                  case EQUAL:
                     ops.add(new Op.Value(new ID.Integer(ic.getEqual())));
                     ops.add(new Op.Binary(Op.BinaryOp.Equal));
                     return Right(new Expression(ops));
                  case IN:
                     HashSet<ID> set = new HashSet<>();
                     for (Long l : ic.getInSetList()) {
                        set.add(new ID.Integer(l.longValue()));
                     }
                     ops.add(new Op.Value(new ID.Set(set)));
                     ops.add(new Op.Binary(Op.BinaryOp.Contains));
                     return Right(new Expression(ops));
                  case NOT_IN:
                     HashSet<ID> set2 = new HashSet<>();
                     for (Long l : ic.getNotInSetList()) {
                        set2.add(new ID.Integer(l.longValue()));
                     }
                     ops.add(new Op.Value(new ID.Set(set2)));
                     ops.add(new Op.Binary(Op.BinaryOp.Contains));
                     ops.add(new Op.Unary(Op.UnaryOp.Negate));
                     return Right(new Expression(ops));
               }
            }
            return Left(new Error.FormatError.DeserializationError("invalid Int constraint"));
         case DATE:
            if(!c.hasDate()) {
               return Left(new Error.FormatError.DeserializationError("invalid Date constraint"));
            } else {
               Schema.DateConstraintV0 ic = c.getDate();

               switch (ic.getKind()) {
                  case BEFORE:
                     ops.add(new Op.Value(new ID.Date(ic.getBefore())));
                     ops.add(new Op.Binary(Op.BinaryOp.LessOrEqual));
                     return Right(new Expression(ops));
                  case AFTER:
                     ops.add(new Op.Value(new ID.Date(ic.getBefore())));
                     ops.add(new Op.Binary(Op.BinaryOp.GreaterOrEqual));
                     return Right(new Expression(ops));
               }
               return Left(new Error.FormatError.DeserializationError("invalid Int constraint"));
            }
         case BYTES:
            if (c.hasBytes()) {
               Schema.BytesConstraintV0 ic = c.getBytes();

               switch (ic.getKind()) {
                  case EQUAL:
                     ops.add(new Op.Value(new ID.Bytes(ic.getEqual().toByteArray())));
                     ops.add(new Op.Binary(Op.BinaryOp.Equal));
                     return Right(new Expression(ops));
                  case IN:
                     HashSet<ID> set = new HashSet<>();
                     for (ByteString l : ic.getInSetList()) {
                        set.add(new ID.Bytes(l.toByteArray()));
                     }
                     ops.add(new Op.Value(new ID.Set(set)));
                     ops.add(new Op.Binary(Op.BinaryOp.Contains));
                     return Right(new Expression(ops));
                  case NOT_IN:
                     HashSet<ID> set2 = new HashSet<>();
                     for (ByteString l : ic.getNotInSetList()) {
                        set2.add(new ID.Bytes(l.toByteArray()));
                     }
                     ops.add(new Op.Value(new ID.Set(set2)));
                     ops.add(new Op.Binary(Op.BinaryOp.Contains));
                     ops.add(new Op.Unary(Op.UnaryOp.Negate));
                     return Right(new Expression(ops));
               }

            }
            return Left(new Error.FormatError.DeserializationError("invalid Bytes constraint"));
         case STRING:
            if (c.hasStr()) {
               Schema.StringConstraintV0 ic = c.getStr();

               switch (ic.getKind()) {
                  case EQUAL:
                     ops.add(new Op.Value(new ID.Str(ic.getEqual())));
                     ops.add(new Op.Binary(Op.BinaryOp.Equal));
                     return Right(new Expression(ops));
                  case PREFIX:
                     ops.add(new Op.Value(new ID.Str(ic.getPrefix())));
                     ops.add(new Op.Binary(Op.BinaryOp.Prefix));
                     return Right(new Expression(ops));
                  case SUFFIX:
                     ops.add(new Op.Value(new ID.Str(ic.getSuffix())));
                     ops.add(new Op.Binary(Op.BinaryOp.Suffix));
                     return Right(new Expression(ops));
                  case REGEX:
                     ops.add(new Op.Value(new ID.Str(ic.getRegex())));
                     ops.add(new Op.Binary(Op.BinaryOp.Regex));
                     return Right(new Expression(ops));
                  case IN:
                     HashSet<ID> set = new HashSet<>();
                     for (String l : ic.getInSetList()) {
                        set.add(new ID.Str(l));
                     }
                     ops.add(new Op.Value(new ID.Set(set)));
                     ops.add(new Op.Binary(Op.BinaryOp.Contains));
                     return Right(new Expression(ops));
                  case NOT_IN:
                     HashSet<ID> set2 = new HashSet<>();
                     for (String l : ic.getNotInSetList()) {
                        set2.add(new ID.Str(l));
                     }
                     ops.add(new Op.Value(new ID.Set(set2)));
                     ops.add(new Op.Binary(Op.BinaryOp.Contains));
                     ops.add(new Op.Unary(Op.UnaryOp.Negate));
                     return Right(new Expression(ops));
               }
            }
            return Left(new Error.FormatError.DeserializationError("invalid String constraint"));
         case SYMBOL:
            if (c.hasSymbol()) {
               Schema.SymbolConstraintV0 ic = c.getSymbol();

               switch (ic.getKind()) {
                  case IN:
                     HashSet<ID> set = new HashSet<>();
                     for (Long l : ic.getInSetList()) {
                        set.add(new ID.Symbol(l.longValue()));
                     }
                     ops.add(new Op.Value(new ID.Set(set)));
                     ops.add(new Op.Binary(Op.BinaryOp.Contains));
                     return Right(new Expression(ops));
                  case NOT_IN:
                     HashSet<ID> set2 = new HashSet<>();
                     for (Long l : ic.getNotInSetList()) {
                        set2.add(new ID.Symbol(l.longValue()));
                     }
                     ops.add(new Op.Value(new ID.Set(set2)));
                     ops.add(new Op.Binary(Op.BinaryOp.Contains));
                     ops.add(new Op.Unary(Op.UnaryOp.Negate));
                     return Right(new Expression(ops));
               }
            }
            return Left(new Error.FormatError.DeserializationError("invalid Symbol constraint"));
      }
      return Left(new Error.FormatError.DeserializationError("invalid constraint kind"));
   }
}
