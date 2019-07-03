package com.clevercloud.biscuit.datalog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.stream.Collectors;

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
         if (!this.ids.get(0).match(other.ids.get(0))) {
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
}
