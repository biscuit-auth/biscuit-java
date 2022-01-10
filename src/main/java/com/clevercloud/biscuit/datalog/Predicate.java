package com.clevercloud.biscuit.datalog;

import biscuit.format.schema.Schema;
import com.clevercloud.biscuit.error.Error;
import io.vavr.control.Either;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

public final class Predicate implements Serializable {
   private final long name;
   private final List<ID> ids;

   public long name() {
      return this.name;
   }

   public final List<ID> ids() {
      return this.ids;
   }

   public final ListIterator<ID> ids_iterator() {
      return this.ids.listIterator();
   }

   public boolean match(final Predicate rule_predicate) {
      if (this.name != rule_predicate.name) {
         return false;
      }
      if (this.ids.size() != rule_predicate.ids.size()) {
         return false;
      }
      for (int i = 0; i < this.ids.size(); ++i) {
         if (!this.ids.get(i).match(rule_predicate.ids.get(i))) {
            return false;
         }
      }
      return true;
   }

   public Predicate clone() {
      final List<ID> ids = new ArrayList<>();
      ids.addAll(this.ids);
      return new Predicate(this.name, ids);
   }

   public Predicate(final long name, final List<ID> ids) {
      this.name = name;
      this.ids = ids;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Predicate predicate = (Predicate) o;
      return name == predicate.name &&
            Objects.equals(ids, predicate.ids);
   }

   @Override
   public int hashCode() {
      return Objects.hash(name, ids);
   }

   @Override
   public String toString() {
      return this.name + "(" + String.join(", ", this.ids.stream().map((i) -> (i == null) ? "(null)" : i.toString()).collect(Collectors.toList())) + ")";
   }

   public Schema.PredicateV2 serialize() {
      Schema.PredicateV2.Builder builder = Schema.PredicateV2.newBuilder()
              .setName(this.name);

      for (int i = 0; i < this.ids.size(); i++) {
         builder.addIds(this.ids.get(i).serialize());
      }

      return builder.build();
   }

   static public Either<Error.FormatError, Predicate> deserializeV2(Schema.PredicateV2 predicate) {
      ArrayList<ID> ids = new ArrayList<>();
      for (Schema.IDV2 id: predicate.getIdsList()) {
         Either<Error.FormatError, ID> res = ID.deserialize_enumV2(id);
         if(res.isLeft()) {
            Error.FormatError e = res.getLeft();
            return Left(e);
         } else {
            ids.add(res.get());
         }
      }

      return Right(new Predicate(predicate.getName(), ids));
   }
}
