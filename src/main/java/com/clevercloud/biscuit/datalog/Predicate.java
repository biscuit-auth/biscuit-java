package com.clevercloud.biscuit.datalog;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

public final class Predicate implements Serializable {
   private final long name;
   private final List<ID> ids;

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

   public Predicate(final long name, final List<ID> ids) {
      this.name = name;
      this.ids = ids;
   }
}
