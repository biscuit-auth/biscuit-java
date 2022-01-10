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

   public final Predicate head() {
      return this.head;
   }

   public final List<Predicate> body() {
      return this.body;
   }

   public final List<Expression> expressions() {
      return this.expressions;
   }

   public void apply(final Set<Fact> facts, final Set<Fact> new_facts, final Set<Long> restricted_symbols) {
      final Set<Long> variables_set = new HashSet<>();
      for (final Predicate pred : this.body) {
         variables_set.addAll(pred.ids().stream().filter((id) -> id instanceof ID.Variable).map((id) -> ((ID.Variable) id).value()).collect(Collectors.toSet()));
      }
      final MatchedVariables variables = new MatchedVariables(variables_set);

      if(this.body.isEmpty()) {
         final Option<Map<Long, ID>> h_opt = variables.check_expressions(this.expressions);
         if(h_opt.isDefined()) {
            final Map<Long, ID> h = h_opt.get();
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

      for (final Map<Long, ID> h : new Combinator(variables, this.body, this.expressions, facts).combine()) {
         final Predicate p = this.head.clone();
         final ListIterator<ID> idit = p.ids_iterator();
         boolean unbound_variable = false;
         while (idit.hasNext()) {
            final ID id = idit.next();
            if (id instanceof ID.Variable) {
               final ID value = h.get(((ID.Variable) id).value());
               idit.set(value);

               // variables that appear in the head should appear in the body and constraints as well
               if(value == null) {
                  unbound_variable = true;
               }
            }
         }

         // if the generated fact has #authority or #ambient as first element and we're n ot in a privileged rule
         // do not generate it
         ID first = p.ids().get(0);
         if(first != null && first instanceof ID.Symbol) {
            if( restricted_symbols.contains(((ID.Symbol) first).value()) ) {
               continue;
            }
         }
         if (!unbound_variable) {
            new_facts.add(new Fact(p));
         }
      }
   }


   // do not produce new facts, only find one matching set of facts
   public boolean test(final Set<Fact> facts) {
      final Set<Long> variables_set = new HashSet<>();
      for (final Predicate pred : this.body) {
         variables_set.addAll(pred.ids().stream().filter((id) -> id instanceof ID.Variable).map((id) -> ((ID.Variable) id).value()).collect(Collectors.toSet()));
      }
      final MatchedVariables variables = new MatchedVariables(variables_set);

      if(this.body.isEmpty()) {
         return variables.check_expressions(this.expressions).isDefined();
      }

      Combinator c = new Combinator(variables, this.body, this.expressions, facts);

      return c.next().isDefined();
   }

   public Rule(final Predicate head, final List<Predicate> body, final List<Expression>  expressions) {
      this.head = head;
      this.body = body;
      this.expressions = expressions;
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

      Either<Error.FormatError, Predicate> res = Predicate.deserializeV2(rule.getHead());
      if(res.isLeft()) {
         Error.FormatError e = res.getLeft();
         return Left(e);
      } else {
         return Right(new Rule(res.get(), body, expressions));
      }
   }
}
