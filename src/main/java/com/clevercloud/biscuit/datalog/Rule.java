package com.clevercloud.biscuit.datalog;

import biscuit.format.schema.Schema;
import com.clevercloud.biscuit.datalog.expressions.Expression;
import com.clevercloud.biscuit.error.Error;
import io.vavr.control.Either;
import io.vavr.control.Option;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

public final class Rule implements Serializable {
   private final Predicate head;
   private final List<Predicate> body;
   private final List<Expression> expressions;
   private final List<Scope> scopes;

   public final Predicate head() {
      return this.head;
   }

   public final List<Predicate> body() {
      return this.body;
   }

   public final List<Expression> expressions() {
      return this.expressions;
   }

   public List<Scope> scopes() {
      return scopes;
   }

   public void apply(final Set<Fact> facts, final Set<Fact> new_facts, SymbolTable symbols) {
      final Set<Long> variables_set = new HashSet<>();
      for (final Predicate pred : this.body) {
         variables_set.addAll(pred.terms().stream().filter((id) -> id instanceof Term.Variable).map((id) -> ((Term.Variable) id).value()).collect(Collectors.toSet()));
      }
      final MatchedVariables variables = new MatchedVariables(variables_set);
      if(this.body.isEmpty()) {
         final Option<Map<Long, Term>> complete_vars_opt = variables.check_expressions(this.expressions, symbols);
         if(complete_vars_opt.isDefined()) {
            final Map<Long, Term> h = complete_vars_opt.get();

            final Predicate p = this.head.clone();
            final ListIterator<Term> idit = p.ids_iterator();
            while (idit.hasNext()) {
               //FIXME: variables that appear in the head should appear in the body and constraints as well
               final Term id = idit.next();
               if (id instanceof Term.Variable) {
                  final Term value = h.get(((Term.Variable) id).value());
                  idit.set(value);
               }
            }

            new_facts.add(new Fact(p));
         }
      }

      Combinator c = new Combinator(variables, this.body, facts, symbols);
      while (true) {
         final Option<MatchedVariables> vars_opt = c.next();
         if(!vars_opt.isDefined()) {
            break;
         }
         MatchedVariables vars = vars_opt.get();
         final Option<Map<Long, Term>> complete_vars_opt = vars.check_expressions(this.expressions, symbols);
         if(complete_vars_opt.isDefined()) {
            final Map<Long, Term> h = complete_vars_opt.get();
            final Predicate p = this.head.clone();
            final ListIterator<Term> idit = p.ids_iterator();
            boolean unbound_variable = false;
            while (idit.hasNext()) {
               final Term id = idit.next();
               if (id instanceof Term.Variable) {
                  final Term value = h.get(((Term.Variable) id).value());
                  idit.set(value);

                  // variables that appear in the head or expressions should appear in the body as well
                  if (value == null) {
                     unbound_variable = true;
                  }
               }
            }

            if (!unbound_variable) {
               new_facts.add(new Fact(p));
            }
         }
      }
   }

   // do not produce new facts, only find one matching set of facts
   public boolean find_match(final Set<Fact> facts, SymbolTable symbols) {
      final Set<Long> variables_set = new HashSet<>();
      for (final Predicate pred : this.body) {
         variables_set.addAll(pred.terms().stream().filter((id) -> id instanceof Term.Variable).map((id) -> ((Term.Variable) id).value()).collect(Collectors.toSet()));
      }
      final MatchedVariables variables = new MatchedVariables(variables_set);

      if(this.body.isEmpty()) {
         return variables.check_expressions(this.expressions, symbols).isDefined();
      }

      Combinator c = new Combinator(variables, this.body,  facts, symbols);

      while(true) {
         Option<MatchedVariables> res = c.next();
         if (res.isDefined()) {
            MatchedVariables vars = res.get();
            if (vars.check_expressions(this.expressions, symbols).isDefined()) {
               return true;
            }
         } else {
            return false;
         }
      }
   }

   // verifies that the expressions return true for every matching set of facts
   public boolean check_match_all(final Set<Fact> facts, SymbolTable symbols) {
      final Set<Long> variables_set = new HashSet<>();
      for (final Predicate pred : this.body) {
         variables_set.addAll(pred.terms().stream().filter((id) -> id instanceof Term.Variable).map((id) -> ((Term.Variable) id).value()).collect(Collectors.toSet()));
      }
      final MatchedVariables variables = new MatchedVariables(variables_set);

      if(this.body.isEmpty()) {
         return variables.check_expressions(this.expressions, symbols).isDefined();
      }

      Combinator c = new Combinator(variables, this.body, facts, symbols);

      boolean found = false;

      while(true) {
         Option<MatchedVariables> res = c.next();
         if (res.isDefined()) {
            // we need at least one match
            found = true;

            MatchedVariables vars = res.get();

            // the expression must succeed for all the matching sets of facts
            if (!vars.check_expressions(this.expressions, symbols).isDefined()) {
               return false;
            }
         } else {
            return found;
         }
      }
   }

   public Rule(final Predicate head, final List<Predicate> body, final List<Expression>  expressions,
               final List<Scope> scopes) {
      this.head = head;
      this.body = body;
      this.expressions = expressions;
      this.scopes = scopes;
   }

   public Schema.RuleV2 serialize() {
      Schema.RuleV2.Builder b = Schema.RuleV2.newBuilder()
              .setHead(this.head.serialize());

      for (int i = 0; i < this.body.size(); i++) {
         b.addBody(this.body.get(i).serialize());
      }

      for (int i = 0; i < this.expressions.size(); i++) {
         b.addExpressions(this.expressions.get(i).serialize());
      }

      for (Scope scope: this.scopes) {
         b.addScope(scope.serialize());
      }

      return b.build();
   }

   static public Either<Error.FormatError, Rule> deserializeV2(Schema.RuleV2 rule) {
      ArrayList<Predicate> body = new ArrayList<>();
      for (Schema.PredicateV2 predicate: rule.getBodyList()) {
         Either<Error.FormatError, Predicate> res = Predicate.deserializeV2(predicate);
         if(res.isLeft()) {
            Error.FormatError e = res.getLeft();
            return Left(e);
         } else {
            body.add(res.get());
         }
      }

      ArrayList<Expression> expressions = new ArrayList<>();
      for (Schema.ExpressionV2 expression: rule.getExpressionsList()) {
         Either<Error.FormatError, Expression> res = Expression.deserializeV2(expression);
         if(res.isLeft()) {
            Error.FormatError e = res.getLeft();
            return Left(e);
         } else {
            expressions.add(res.get());
         }
      }

      ArrayList<Scope> scopes = new ArrayList<>();
      for (Schema.Scope scope: rule.getScopeList()) {
         Either<Error.FormatError, Scope> res = Scope.deserialize(scope);
         if(res.isLeft()) {
            Error.FormatError e = res.getLeft();
            return Left(e);
         } else {
            scopes.add(res.get());
         }
      }

      Either<Error.FormatError, Predicate> res = Predicate.deserializeV2(rule.getHead());
      if(res.isLeft()) {
         Error.FormatError e = res.getLeft();
         return Left(e);
      } else {
         return Right(new Rule(res.get(), body, expressions, scopes));
      }
   }
}
