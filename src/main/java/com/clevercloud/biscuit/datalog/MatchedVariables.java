package com.clevercloud.biscuit.datalog;

import javax.swing.text.html.Option;
import java.io.Serializable;
import java.util.*;

public final class MatchedVariables implements Serializable {
   private final Map<Long, Optional<ID>> variables;

   public boolean insert(final long key, final ID value) {
      if (this.variables.containsKey(key)) {
         final Optional<ID> val = this.variables.get(key);
         if (val.isPresent()) {
            return val.get().equals(value);
         } else {
            this.variables.put(key, Optional.of(value));
            return true;
         }
      } else {
         return false;
      }
   }

   public boolean is_complete() {
      return this.variables.values().stream().allMatch((v) -> v.isPresent());
   }

   public Optional<Map<Long, ID>> complete() {
      final Map<Long, ID> variables = new HashMap<>();
      for (final Map.Entry<Long, Optional<ID>> entry : this.variables.entrySet()) {
         if (entry.getValue().isPresent()) {
            variables.put(entry.getKey(), entry.getValue().get());
         } else {
            return Optional.empty();
         }
      }
      return Optional.of(variables);
   }

   public MatchedVariables clone() {
      final MatchedVariables other = new MatchedVariables(this.variables.keySet());
      for (final Map.Entry<Long, Optional<ID>> entry : this.variables.entrySet()) {
         if (entry.getValue().isPresent()) {
            other.variables.put(entry.getKey(), entry.getValue());
         }
      }
      return other;
   }

   public MatchedVariables(final Set<Long> ids) {
      this.variables = new HashMap<>();
      for (final Long id : ids) {
         this.variables.put(id, Optional.empty());
      }
   }
}
