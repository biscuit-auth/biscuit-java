package com.clevercloud.biscuit.datalog;

import com.clevercloud.biscuit.datalog.constraints.Constraint;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class Combinator implements Serializable {
   private final MatchedVariables variables;
   private final List<Predicate> predicates;
   private final List<Constraint> constraints;
   private final Set<Fact> all_facts;
   private final Set<Fact> current_facts;

   public Combinator(final MatchedVariables variables, final List<Predicate> predicates, final List<Constraint> constraints, final Set<Fact> facts) {
      this.variables = variables;
      this.predicates = predicates;
      this.constraints = constraints;
      this.all_facts = facts;
      this.current_facts = facts.stream().filter((fact) -> fact.match_predicate(predicates.get(0))).collect(Collectors.toSet());
   }
}
