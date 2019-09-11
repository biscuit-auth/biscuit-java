package com.clevercloud.biscuit.datalog;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import io.vavr.control.Option;

public final class SymbolTable implements Serializable {
   public final List<String> symbols;

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

   public Option<Long> get(final String symbol) {
      long index = this.symbols.indexOf(symbol);
      if (index == -1) {
         return Option.none();
      } else {
         return Option.some(index);
      }
   }

   public String print_rule(final Rule r) {
      final String res = this.print_predicate(r.head());
      final List<String> preds = r.body().stream().map((p) -> this.print_predicate(p)).collect(Collectors.toList());
      final List<String> constraints = r.constraints().stream().map((c) -> c.toString()).collect(Collectors.toList());

      return res + " <- " + String.join(" && ", preds) + " | " + String.join(" && ", constraints);
   }

   public String print_predicate(final Predicate p) {
      List<String> ids = p.ids().stream().map((i) -> {
         if (i instanceof ID.Variable) {
            return ((ID.Variable) i).value() + "?";
         } else if (i instanceof ID.Symbol) {
            return "#" + this.symbols.get((int) ((ID.Symbol) i).value());
         } else if (i instanceof ID.Date) {
            return Date.from(Instant.ofEpochSecond(((ID.Date) i).value())).toString();
         } else if (i instanceof ID.Integer) {
            return "" + ((ID.Integer) i).value();
         } else if (i instanceof ID.Str) {
            return "\""+((ID.Str) i).value()+"\"";
         } else {
            return "???";
         }
      }).collect(Collectors.toList());
      return Optional.ofNullable(this.symbols.get((int) p.name())).orElse("<?>") + "(" + String.join(", ", ids) + ")";
   }

   public String print_fact(final Fact f) {
      return this.print_predicate(f.predicate());
   }

   public SymbolTable() {
      this.symbols = new ArrayList<>();
   }
   public SymbolTable(SymbolTable s) {
      this.symbols = new ArrayList<>();
      for(String symbol: s.symbols) {
         this.symbols.add(symbol);
      }
   }
}
