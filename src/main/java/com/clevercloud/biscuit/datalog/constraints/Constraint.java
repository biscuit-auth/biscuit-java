package com.clevercloud.biscuit.datalog.constraints;

import java.io.Serializable;

public final class Constraint implements Serializable {
   private final long id;
   private final ConstraintKind kind;

   public Constraint(long id, ConstraintKind kind) {
      this.id = id;
      this.kind = kind;
   }
}
