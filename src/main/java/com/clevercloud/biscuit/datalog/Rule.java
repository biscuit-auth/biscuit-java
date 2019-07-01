package com.clevercloud.biscuit.datalog;

import com.clevercloud.biscuit.datalog.constraints.Constraint;

import java.io.Serializable;
import java.util.List;

public final class Rule implements Serializable {
   private final Predicate head;
   private final List<Predicate> body;
   private final List<Constraint> constraints;

   public Rule(Predicate head, List<Predicate> body, List<Constraint> constraints) {
      this.head = head;
      this.body = body;
      this.constraints = constraints;
   }
}
