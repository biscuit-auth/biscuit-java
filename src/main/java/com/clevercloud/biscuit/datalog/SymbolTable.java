package com.clevercloud.biscuit.datalog;

import com.clevercloud.biscuit.datalog.constraints.Constraint;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class SymbolTable implements Serializable {
   private final List<String> symbols;

   public long insert(final String symbol) {
      int index = this.symbols.indexOf(symbol);
      if (index == -1) {
         this.symbols.add(symbol);
         return this.symbols.size() - 1;
      } else {
         return index;
      }
   }

   public ID add(final String symbol) {
      return new ID.Symbol(this.insert(symbol));
   }

   public String print_rule(final Rule r) {
      final String res = this.print_predicate(r.head());
      final List<String> preds = r.body().stream().map((p) -> this.print_predicate(p)).collect(Collectors.toList());
      final List<String> constraints = r.constraints().stream().map((c) -> c.toString()).collect(Collectors.toList());

      return res + " <- " + String.join(" && ", preds) + " | " + String.join(" && ", constraints);
   }

   public String print_predicate(final Predicate p) {
      List<String> ids = p.ids().stream().map((i) -> i.toString()).collect(Collectors.toList());
      return Optional.ofNullable(this.symbols.get((int)p.name())).orElse("<?>") + "(" + String.join(", ", ids) + ")";
   }

   public SymbolTable() {
      this.symbols = new ArrayList<>();
   }
}
