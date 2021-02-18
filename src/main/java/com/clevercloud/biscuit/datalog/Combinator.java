package com.clevercloud.biscuit.datalog;

import com.clevercloud.biscuit.datalog.expressions.Expression;
import io.vavr.control.Option;

import java.io.Serializable;
import java.util.*;

public final class Combinator implements Serializable {
   private final MatchedVariables variables;
   private final List<Predicate> next_predicates;
   private final List<Expression> expressions;
   private final Set<Fact> all_facts;
   private final Predicate pred;
   private final Iterator<Fact> fit;
   private Combinator current_it;


   public Option<MatchedVariables> next() {
      while(true) {
         if (this.current_it != null) {
            Option<MatchedVariables> next_vars_opt = this.current_it.next();
            // the iterator is empty, try with the next fact
            if(next_vars_opt.isEmpty()) {
               this.current_it = null;
               continue;
            }

            MatchedVariables next_vars = next_vars_opt.get();

            final Option<Map<Long, ID>> v_opt = this.check_expressions(next_vars);
            if(v_opt.isEmpty()) {
               continue;
            } else {
               return Option.some(next_vars);
            }
         }

         // we iterate over the facts that match the current predicate
         if (this.fit.hasNext()) {
            final Fact current_fact = this.fit.next();

            // create a new MatchedVariables in which we fix variables we could unify from our first predicate and the current fact
            MatchedVariables vars = this.variables.clone();
            boolean match_ids = true;

            // we know the fact matches the predicate's format so they have the same number of terms
            // fill the MatchedVariables before creating the next combinator
            for (int i = 0; i < pred.ids().size(); ++i) {
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

            // there are no more predicates to check
            if (next_predicates.isEmpty()) {
               final Option<Map<Long, ID>> v_opt = this.check_expressions(vars);
               if(v_opt.isEmpty()) {
                  continue;
               } else {
                  return Option.some(vars);
               }
            } else {
               // we found a matching fact, we create a new combinator over the rest of the predicates
               // no need to copy all of the expressions at all levels
               this.current_it = new Combinator(vars, next_predicates, new ArrayList<>(), this.all_facts);
            }
         } else {
            break;
         }
      }

      return Option.none();
   }

   public List<Map<Long, ID>> combine() {
      final List<Map<Long, ID>> variables = new ArrayList<>();

      while(true) {
         Option<MatchedVariables> res = this.next();

         if(res.isEmpty()) {
            return variables;
         }

         Optional<Map<Long, ID>> vars = res.get().complete();
         if(vars.isPresent()) {
            variables.add(vars.get());
         }
      }
   }

   public Combinator(final MatchedVariables variables, final List<Predicate> predicates, final List<Expression> expressions,
                     final Set<Fact> all_facts) {
      this.variables = variables;
      this.expressions = expressions;
      this.all_facts = all_facts;
      this.current_it = null;
      this.pred = predicates.get(0);
      this.fit = all_facts.stream().filter((fact) -> fact.match_predicate(predicates.get(0))).iterator();

      final List<Predicate> next_predicates = new ArrayList<>();
      for (int i = 1; i < predicates.size(); ++i) {
         next_predicates.add(predicates.get(i));
      }
      this.next_predicates = next_predicates;
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
