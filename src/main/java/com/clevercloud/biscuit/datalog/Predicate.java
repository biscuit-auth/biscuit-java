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

   public boolean match(final Predicate other) {
      if (this.name != other.name) {
         return false;
      }
      if (this.ids.size() != other.ids.size()) {
         return false;
      }
      for (int i = 0; i < this.ids.size(); ++i) {
         if (!this.ids.get(i).match(other.ids.get(i))) {
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
      return this.name + "(" + String.join(", ", this.ids.stream().map((i) -> (i == null) ? "(null)" : i.toString()).collect(Collectors.toSet())) + ")";
   }

   public Schema.Predicate serialize() {
      Schema.Predicate.Builder builder = Schema.Predicate.newBuilder()
              .setName(this.name);

      for (int i = 0; i < this.ids.size(); i++) {
         builder.addIds(this.ids.get(i).serialize());
      }

      return builder.build();
   }

   static public Either<Error.FormatError, Predicate> deserialize(Schema.Predicate predicate) {
      ArrayList<ID> ids = new ArrayList<>();
      for (Schema.ID id: predicate.getIdsList()) {
         Either<Error.FormatError, ID> res = ID.deserialize_enum(id);
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
