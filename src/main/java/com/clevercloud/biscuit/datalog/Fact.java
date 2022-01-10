package com.clevercloud.biscuit.datalog;

import biscuit.format.schema.Schema;
import com.clevercloud.biscuit.error.Error;
import io.vavr.control.Either;

import java.io.Serializable;
import java.util.Objects;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

public final class Fact implements Serializable {
   private final Predicate predicate;

   public final Predicate predicate() {
      return this.predicate;
   }

   public boolean match_predicate(final Predicate rule_predicate) {
      return this.predicate.match(rule_predicate);
   }

   public Fact(final Predicate predicate) {
      this.predicate = predicate;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Fact fact = (Fact) o;
      return Objects.equals(predicate, fact.predicate);
   }

   @Override
   public int hashCode() {
      return Objects.hash(predicate);
   }

   @Override
   public String toString() {
      return this.predicate.toString();
   }

   public Schema.FactV2 serialize() {
      return Schema.FactV2.newBuilder()
              .setPredicate(this.predicate.serialize())
              .build();
   }

   static public Either<Error.FormatError, Fact> deserializeV2(Schema.FactV2 fact) {
      Either<Error.FormatError, Predicate> res = Predicate.deserializeV2(fact.getPredicate());
      if(res.isLeft()) {
         Error.FormatError e = res.getLeft();
         return Left(e);
      } else {
         return Right(new Fact(res.get()));
      }
   }
}
