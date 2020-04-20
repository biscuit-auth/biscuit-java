package com.clevercloud.biscuit.datalog;

import biscuit.format.schema.Schema;
import com.clevercloud.biscuit.datalog.constraints.Constraint;
import com.clevercloud.biscuit.error.Error;
import io.vavr.control.Either;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

public final class Rule implements Serializable {
   private final Predicate head;
   private final List<Predicate> body;
   private final List<Constraint> constraints;

   public Rule(Rule that) {
      this.head = that.head.clone();
      List<Predicate> body = new ArrayList<>();
      for (Predicate p: that.body) {
         body.add(p.clone());
      }
      this.body = body;
      List<Constraint> constraints = new ArrayList<>();
      for (Constraint c: that.constraints) {
         constraints.add(new Constraint(c.id, c.kind));
      }
      this.constraints = constraints;
   }

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

   public Schema.Rule serialize() {
      Schema.Rule.Builder b = Schema.Rule.newBuilder()
              .setHead(this.head.serialize());

      for (int i = 0; i < this.body.size(); i++) {
         b.addBody(this.body.get(i).serialize());
      }

      for (int i = 0; i < this.constraints.size(); i++) {
         b.addConstraints(this.constraints.get(i).serialize());
      }

      return b.build();
   }

   static public Either<Error.FormatError, Rule> deserialize(Schema.Rule rule) {
      ArrayList<Predicate> body = new ArrayList<>();
      for (Schema.Predicate predicate: rule.getBodyList()) {
         Either<Error.FormatError, Predicate> res = Predicate.deserialize(predicate);
         if(res.isLeft()) {
            Error.FormatError e = res.getLeft();
            return Left(e);
         } else {
            body.add(res.get());
         }
      }

      ArrayList<Constraint> constraints = new ArrayList<>();
      for (Schema.Constraint constraint: rule.getConstraintsList()) {
         Either<Error.FormatError, Constraint> res = Constraint.deserialize(constraint);
         if(res.isLeft()) {
            Error.FormatError e = res.getLeft();
            return Left(e);
         } else {
            constraints.add(res.get());
         }
      }

      Either<Error.FormatError, Predicate> res = Predicate.deserialize(rule.getHead());
      if(res.isLeft()) {
         Error.FormatError e = res.getLeft();
         return Left(e);
      } else {
         return Right(new Rule(res.get(), body, constraints));
      }
   }
}
