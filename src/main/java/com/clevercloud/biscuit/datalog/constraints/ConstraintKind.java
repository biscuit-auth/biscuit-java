package com.clevercloud.biscuit.datalog.constraints;

import java.io.Serializable;

public abstract class ConstraintKind implements Serializable {
   public static final class Int extends ConstraintKind implements Serializable {
      private final IntConstraint constraint;

      public boolean check(final long value) {
         return this.constraint.check(value);
      }

      public Int(final IntConstraint constraint) {
         this.constraint = constraint;
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
   }

   public static final class Date extends ConstraintKind implements Serializable {
      private final DateConstraint constraint;

      public boolean check(final long value) {
         return this.constraint.check(value);
      }

      public Date(final DateConstraint constraint) {
         this.constraint = constraint;
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
   }
}
