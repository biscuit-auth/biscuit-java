package com.clevercloud.biscuit.datalog;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;

public final class MatchedVariables implements Serializable {
   private final HashMap<Long, Optional<ID>> variables;

   public MatchedVariables(final HashSet<Long> ids) {
      this.variables = new HashMap<>();
      for (final Long id : ids) {
         this.variables.putIfAbsent(id, Optional.empty());
      }
   }
}
