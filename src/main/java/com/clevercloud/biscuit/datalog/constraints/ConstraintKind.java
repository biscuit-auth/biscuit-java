package com.clevercloud.biscuit.datalog.constraints;

import java.io.Serializable;

public abstract class ConstraintKind implements Serializable {
   public final class Int extends ConstraintKind implements Serializable {
      private final IntConstraint constraint;

      public boolean check(long value) {
         return this.constraint.check(value);
      }

      public Int(IntConstraint constraint) {
         this.constraint = constraint;
      }
   }

   public final class Str extends ConstraintKind implements Serializable {
      private final StrConstraint constraint;

      public boolean check(String value) {
         return this.constraint.check(value);
      }

      public Str(StrConstraint constraint) {
         this.constraint = constraint;
      }
   }

   public final class Date extends ConstraintKind implements Serializable {
      private final DateConstraint constraint;

      public boolean check(long value) {
         return this.constraint.check(value);
      }

      public Date(DateConstraint constraint) {
         this.constraint = constraint;
      }
   }

   public final class Symbol extends ConstraintKind implements Serializable {
      private final SymbolConstraint constraint;

      public boolean check(long value) {
         return this.constraint.check(value);
      }

      public Symbol(SymbolConstraint constraint) {
         this.constraint = constraint;
      }
   }
}
