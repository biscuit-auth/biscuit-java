package com.clevercloud.biscuit.datalog;

import com.clevercloud.biscuit.datalog.expressions.Expression;
import io.vavr.control.Option;

import java.io.Serializable;
import java.util.*;

public final class Combinator implements Serializable {
   private final MatchedVariables variables;
   private final List<Predicate> next_predicates;
   private final Set<Fact> all_facts;
   private final Predicate pred;
   private final Iterator<Fact> fit;
   private Combinator current_it;
   private final SymbolTable symbols;

   public Option<MatchedVariables> next() {
      while(true) {
         if (this.current_it != null) {
            Option<MatchedVariables> next_vars_opt = this.current_it.next();
            // the iterator is empty, try with the next fact
            if(next_vars_opt.isEmpty()) {
               this.current_it = null;
               continue;
            }
            return next_vars_opt;
         }

         // we iterate over the facts that match the current predicate
         if (this.fit.hasNext()) {
            final Fact current_fact = this.fit.next();

            // create a new MatchedVariables in which we fix variables we could unify from our first predicate and the current fact
            MatchedVariables vars = this.variables.clone();
            boolean match_ids = true;

            // we know the fact matches the predicate's format so they have the same number of terms
            // fill the MatchedVariables before creating the next combinator
            for (int i = 0; i < pred.terms().size(); ++i) {
               final Term id = pred.terms().get(i);
               if (id instanceof Term.Variable) {
                  final long key = ((Term.Variable) id).value();
                  final Term value = current_fact.predicate().terms().get(i);

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
               final Option<Map<Long, Term>> v_opt = vars.complete();
               if(v_opt.isEmpty()) {
                  continue;
               } else {
                  return Option.some(vars);
               }
            } else {
               // we found a matching fact, we create a new combinator over the rest of the predicates
               // no need to copy all of the expressions at all levels
               this.current_it = new Combinator(vars, next_predicates, this.all_facts, this.symbols);
            }
         } else {
            break;
         }
      }

      return Option.none();
   }

   public List<Map<Long, Term>> combine() {
      final List<Map<Long, Term>> variables = new ArrayList<>();

      while(true) {
         Option<MatchedVariables> res = this.next();

         if(res.isEmpty()) {
            return variables;
         }

         Option<Map<Long, Term>> vars = res.get().complete();
         if(vars.isDefined()) {
            variables.add(vars.get());
         }
      }
   }

   public Combinator(final MatchedVariables variables, final List<Predicate> predicates,
                     final Set<Fact> all_facts, final SymbolTable symbols) {
      this.variables = variables;
      this.all_facts = all_facts;
      this.current_it = null;
      this.pred = predicates.get(0);
      this.fit = all_facts.stream().filter((fact) -> fact.match_predicate(predicates.get(0))).iterator();
      this.symbols = symbols;

      final List<Predicate> next_predicates = new ArrayList<>();
      for (int i = 1; i < predicates.size(); ++i) {
         next_predicates.add(predicates.get(i));
      }
      this.next_predicates = next_predicates;
   }
}
