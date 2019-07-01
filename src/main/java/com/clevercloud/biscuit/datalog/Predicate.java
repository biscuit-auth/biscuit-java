package com.clevercloud.biscuit.datalog;

import java.io.Serializable;
import java.util.List;

public final class Predicate implements Serializable {
   private final long name;
   private final List<ID> ids;

   public Predicate(final long name, final List<ID> ids) {
      this.name = name;
      this.ids = ids;
   }
}
