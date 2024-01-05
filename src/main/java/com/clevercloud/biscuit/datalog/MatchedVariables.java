package com.clevercloud.biscuit.datalog;

import com.clevercloud.biscuit.datalog.expressions.Expression;
import io.vavr.control.Option;

import java.io.Serializable;
import java.util.*;

public final class MatchedVariables implements Serializable {
   private final Map<Long, Optional<Term>> variables;

   public boolean insert(final long key, final Term value) {
      if (this.variables.containsKey(key)) {
         final Optional<Term> val = this.variables.get(key);
         if (val.isPresent()) {
            return val.get().equals(value);
         } else {
            this.variables.put(key, Optional.of(value));
            return true;
         }
      } else {
         return false;
      }
   }

   public Optional<Term> get(final long key) {
      return this.variables.get(key);
   }

   public boolean is_complete() {
      return this.variables.values().stream().allMatch((v) -> v.isPresent());
   }

   public Option<Map<Long, Term>> complete() {
      final Map<Long, Term> variables = new HashMap<>();
      for (final Map.Entry<Long, Optional<Term>> entry : this.variables.entrySet()) {
         if (entry.getValue().isPresent()) {
            variables.put(entry.getKey(), entry.getValue().get());
         } else {
            return Option.none();
         }
      }
      return Option.some(variables);
   }

   public MatchedVariables clone() {
      final MatchedVariables other = new MatchedVariables(this.variables.keySet());
      for (final Map.Entry<Long, Optional<Term>> entry : this.variables.entrySet()) {
         if (entry.getValue().isPresent()) {
            other.variables.put(entry.getKey(), entry.getValue());
         }
      }
      return other;
   }

   public MatchedVariables(final Set<Long> ids) {
      this.variables = new HashMap<>();
      for (final Long id : ids) {
         this.variables.put(id, Optional.empty());
      }
   }

   public Option<Map<Long, Term>> check_expressions(List<Expression> expressions, SymbolTable symbols) {
      final Option<Map<Long, Term>> vars = this.complete();
      if (vars.isDefined()) {
         Map<Long, Term> variables = vars.get();


         for(Expression e: expressions) {
            Option<Term> res = e.evaluate(variables, new TemporarySymbolTable(symbols));

            if(res.isEmpty()) {
               return Option.none();
            }

            if(!res.get().equals(new Term.Bool(true))) {
               return Option.none();
            }
         }

         return Option.some(variables);
      } else {
         return Option.none();
      }
   }
}
