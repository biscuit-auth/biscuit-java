package com.clevercloud.biscuit.datalog.constraints;

import com.clevercloud.biscuit.datalog.ID;

import java.io.Serializable;

public final class Constraint implements Serializable {
   private final long id;
   private final ConstraintKind kind;

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
      return this.id + "? " + this.kind.toString();
   }
}
