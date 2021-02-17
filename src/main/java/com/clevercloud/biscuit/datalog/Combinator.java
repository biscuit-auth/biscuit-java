package com.clevercloud.biscuit.datalog;

import com.clevercloud.biscuit.datalog.constraints.Constraint;
import com.clevercloud.biscuit.datalog.expressions.Expression;
import io.vavr.control.Option;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public final class Combinator implements Serializable {
   private final MatchedVariables variables;
   private final List<Predicate> predicates;
   private final List<Expression> expressions;
   private final Set<Fact> all_facts;
   private final Set<Fact> current_facts;

   public List<Map<Long, ID>> combine() {
      final List<Map<Long, ID>> variables = new ArrayList<>();
      if (this.predicates.isEmpty()) {
         final Option<Map<Long, ID>> vars = this.check_expressions(this.variables);
         if (vars.isDefined()) {
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
                  final List<Map<Long, ID>> next = new Combinator(vars, next_predicates, this.expressions, this.all_facts).combine();
                  if (next.isEmpty()) {
                     return variables;
                  }
                  variables.addAll(next);
               }
            } else {
               final Option<Map<Long, ID>> v = this.check_expressions(vars);
               if (v.isDefined()) {
                  variables.add(v.get());
               } else {
                  continue;
               }
            }
         }
      }
      return variables;
   }

   public Combinator(final MatchedVariables variables, final List<Predicate> predicates, final List<Expression> expressions,
                     final Set<Fact> facts) {
      this.variables = variables;
      this.predicates = predicates;
      this.expressions = expressions;
      this.all_facts = facts;
      this.current_facts = facts.stream().filter((fact) -> fact.match_predicate(predicates.get(0))).collect(Collectors.toSet());
   }

   public Option<Map<Long, ID>> check_expressions(MatchedVariables matched_variables) {
      final Optional<Map<Long, ID>> vars = matched_variables.complete();
      if (vars.isPresent()) {
         Map<Long, ID> variables = vars.get();

         for(Expression e: this.expressions) {
            Option<ID> res = e.evaluate(variables);
            if(res.isEmpty()) {
               return Option.none();
            }

            if(res.get() != new ID.Bool(true)) {
               return Option.none();
            }
         }

         return Option.some(variables);
      } else {
         return Option.none();
      }
   }
}
