package com.clevercloud.biscuit.datalog;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.clevercloud.biscuit.datalog.constraints.Constraint;
import com.clevercloud.biscuit.datalog.constraints.ConstraintKind;
import com.clevercloud.biscuit.datalog.constraints.SymbolConstraint;
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
      String res = "*"+this.print_predicate(r.head());
      final List<String> preds = r.body().stream().map((p) -> "!"+this.print_predicate(p)).collect(Collectors.toList());
      final List<String> constraints = r.constraints().stream().map((c) -> this.print_constraint(c)).collect(Collectors.toList());

      res += " <- " + String.join(", ", preds);
      if(!constraints.isEmpty()) {
         res += " @ " + String.join(", ", constraints);
      }
      return res;
   }

   public String print_constraint(final Constraint c) {
      String res = "$" + c.id + " ";
      if (c.kind instanceof ConstraintKind.Int) {
         res += c.kind.toString();
      } else if (c.kind instanceof ConstraintKind.Str) {
         res += c.kind.toString();
      } else if (c.kind instanceof ConstraintKind.Date) {
         res += c.kind.toString();
      } else if (c.kind instanceof  ConstraintKind.Symbol) {
         SymbolConstraint s = ((ConstraintKind.Symbol) c.kind).constraint;

         if (s instanceof SymbolConstraint.InSet) {
            List<String> set = ((SymbolConstraint.InSet) s).value.stream().map((sym) -> "#" + this.symbols.get(sym.intValue())).collect(Collectors.toList());
            res += "in " + set.toString();
         } else if (s instanceof SymbolConstraint.NotInSet) {
            List<String> set = ((SymbolConstraint.NotInSet) s).value.stream().map((sym) -> "#" + this.symbols.get(sym.intValue())).collect(Collectors.toList());
            res += "not in " + set.toString();
         }
      }

      return res;
   }


   public String print_predicate(final Predicate p) {
      List<String> ids = p.ids().stream().map((i) -> {
         if (i instanceof ID.Variable) {
            return "$" + ((ID.Variable) i).value();
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
      return "!" + this.print_predicate(f.predicate());
   }

   public String print_caveat(final Caveat c) {
      final List<String> queries = c.queries().stream().map((q) -> this.print_rule(q)).collect(Collectors.toList());
      return String.join(" || ", queries);
   }

   public String print_world(final World w) {
      final List<String> facts = w.facts().stream().map((f) -> this.print_fact(f)).collect(Collectors.toList());
      final List<String> rules = w.rules().stream().map((r) -> this.print_rule(r)).collect(Collectors.toList());
      final List<String> caveatsStr = w.caveats().stream().map((c) -> this.print_caveat(c)).collect(Collectors.toList());

      StringBuilder b = new StringBuilder();
      b.append("World {\n\tfacts: [");
      b.append(String.join(",\n\t\t", facts));
      b.append("\n\t],\n\trules: [");
      b.append(String.join(",\n\t\t", rules));
      b.append("\n\t],\n\tcaveats: [");
      b.append(String.join(",\n\t\t", caveatsStr));
      b.append("\n\t]\n}");

      return b.toString();
   }

   public String print_symbol(int i) {
      return this.symbols.get(i);
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
