package com.clevercloud.biscuit.datalog;

import com.clevercloud.biscuit.datalog.constraints.Constraint;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public final class Combinator implements Serializable {
   private final MatchedVariables variables;
   private final List<Predicate> predicates;
   private final List<Constraint> constraints;
   private final Set<Fact> all_facts;
   private final Set<Fact> current_facts;

   public List<Map<Long, ID>> combine() {
      final List<Map<Long, ID>> variables = new ArrayList<>();
      if (this.predicates.isEmpty()) {
         final Optional<Map<Long, ID>> vars = this.variables.complete();
         if (vars.isPresent()) {
            variables.add(vars.get());
         }
         return variables;
      }
      final ListIterator<Predicate> pit = this.predicates.listIterator();
      while (pit.hasNext()) {
         if (this.current_facts.isEmpty()) {
            return variables;
         }
         final Predicate pred = pit.next();
         for (final Fact current_fact : this.current_facts) {
            // create a new MatchedVariables in which we fix variables we could unify from our first predicate and the current fact
            final MatchedVariables vars = this.variables.clone();
            boolean match_ids = true;
            final int min_size = Math.min(pred.ids().size(), current_fact.predicate().ids().size());
            for (int i = 0; i < min_size; ++i) {
               final ID id = pred.ids().get(i);
               if (id instanceof ID.Variable) {
                  final long key = ((ID.Variable) id).value();
                  final ID value = current_fact.predicate().ids().get(i);
                  for (final Constraint c : this.constraints) {
                     if (!c.check(key, value)) {
                        match_ids = false;
                        break;
                     }
                  }
                  if (!vars.insert(key, value)) {
                     match_ids = false;
                  }
                  if (!match_ids) {
                     break;
                  }
               }
            }

            if (!match_ids) {
               continue;
            }

            if (pit.hasNext()) {
               final List<Predicate> next_predicates = new ArrayList<>();
               for (int i = pit.nextIndex(); i < this.predicates.size(); ++i) {
                  next_predicates.add(this.predicates.get(i));
               }
               if (!next_predicates.isEmpty()) {
                  final List<Map<Long, ID>> next = new Combinator(vars, next_predicates, this.constraints, this.all_facts).combine();
                  if (next.isEmpty()) {
                     return variables;
                  }
                  variables.addAll(next);
               }
            } else {
               final Optional<Map<Long, ID>> v = vars.complete();
               if (v.isPresent()) {
                  variables.add(v.get());
               } else {
                  continue;
               }
            }
         }
      }
      return variables;
   }

   public Combinator(final MatchedVariables variables, final List<Predicate> predicates, final List<Constraint> constraints, final Set<Fact> facts) {
      this.variables = variables;
      this.predicates = predicates;
      this.constraints = constraints;
      this.all_facts = facts;
      this.current_facts = facts.stream().filter((fact) -> fact.match_predicate(predicates.get(0))).collect(Collectors.toSet());
   }
}
