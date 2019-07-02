package com.clevercloud.biscuit.datalog;

import com.clevercloud.biscuit.datalog.constraints.Constraint;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public final class Rule implements Serializable {
   private final Predicate head;
   private final List<Predicate> body;
   private final List<Constraint> constraints;

   public final Predicate head() {
      return this.head;
   }

   public final List<Predicate> body() {
      return this.body;
   }

   public final List<Constraint> constraints() {
      return this.constraints;
   }

   public void apply(final Set<Fact> facts, final Set<Fact> new_facts) {
      final Set<Long> variables_set = new HashSet<>();
      for (final Predicate pred : this.body) {
         variables_set.addAll(pred.ids().stream().filter((id) -> id instanceof ID.Variable).map((id) -> ((ID.Variable) id).value()).collect(Collectors.toSet()));
      }
      final MatchedVariables variables = new MatchedVariables(variables_set);
      for (final Map<Long, ID> h : new Combinator(variables, this.body, this.constraints, facts).combine()) {
         final Predicate p = this.head.clone();
         final ListIterator<ID> idit = p.ids_iterator();
         while (idit.hasNext()) {
            //FIXME: variables that appear in the head should appear in the body and constraints as well
            final ID id = idit.next();
            if (id instanceof ID.Variable) {
               final ID value = h.get(((ID.Variable) id).value());
               idit.set(value);
            }
         }
         new_facts.add(new Fact(p));
      }
   }

   public Rule(final Predicate head, final List<Predicate> body, final List<Constraint> constraints) {
      this.head = head;
      this.body = body;
      this.constraints = constraints;
   }
}
