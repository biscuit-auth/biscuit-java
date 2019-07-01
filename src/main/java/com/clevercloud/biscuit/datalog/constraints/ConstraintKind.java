package com.clevercloud.biscuit.datalog.constraints;

import java.io.Serializable;

public abstract class ConstraintKind implements Serializable {
   public final class Int extends ConstraintKind implements Serializable {
      private final IntConstraint constraint;

      public Int(IntConstraint constraint) {
         this.constraint = constraint;
      }
   }

   public final class Str extends ConstraintKind implements Serializable {
      private final StrConstraint constraint;

      public Str(StrConstraint constraint) {
         this.constraint = constraint;
      }
   }

   public final class Date extends ConstraintKind implements Serializable {
      private final DateConstraint constraint;

      public Date(DateConstraint constraint) {
         this.constraint = constraint;
      }
   }

   public final class Symbol extends ConstraintKind implements Serializable {
      private final SymbolConstraint constraint;

      public Symbol(SymbolConstraint constraint) {
         this.constraint = constraint;
      }
   }
}
