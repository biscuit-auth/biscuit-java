package com.clevercloud.biscuit.datalog;

import com.clevercloud.biscuit.error.Error;
import io.vavr.control.Either;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

public final class World implements Serializable {
   private final Set<Fact> facts;
   private final List<Rule> rules;
   private final List<Rule> privileged_rules;
   private final List<Check> checks;

   public void add_fact(final Fact fact) {
      this.facts.add(fact);
   }

   public void add_rule(final Rule rule) {
      this.rules.add(rule);
   }

   public void add_privileged_rule(final Rule rule) {
      this.privileged_rules.add(rule);
   }

   public void add_check(Check check) { this.checks.add(check); }

   public void clearRules() {
      this.rules.clear();
   }

   public Either<Error, Void> run(final SymbolTable symbols) {
      return this.run(new RunLimits(), symbols);
   }

   public Either<Error, Void> run(RunLimits limits, final SymbolTable symbols) {
      int iterations = 0;
      Instant limit = Instant.now().plus(limits.maxTime);

      while(true) {
         final Set<Fact> new_facts = new HashSet<>();

         for (final Rule rule : this.privileged_rules) {
            rule.apply(this.facts, new_facts, symbols);

            if(Instant.now().compareTo(limit) >= 0) {
               return Left(new Error.Timeout());
            }
         }

         for (final Rule rule : this.rules) {
            rule.apply(this.facts, new_facts, symbols);

            if(Instant.now().compareTo(limit) >= 0) {
               return Left(new Error.Timeout());
            }
         }

         final int len = this.facts.size();
         this.facts.addAll(new_facts);
         if (this.facts.size() == len) {
            return Right(null);
         }

         if (this.facts.size() >= limits.maxFacts) {
            return Left(new Error.TooManyFacts());
         }

         iterations += 1;
         if(iterations >= limits.maxIterations) {
            return Left(new Error.TooManyIterations());
         }
      }
   }

   public final Set<Fact> facts() {
      return this.facts;
   }

   public List<Rule> privileged_rules() { return this.privileged_rules; }

   public List<Rule> rules() { return this.rules; }

   public List<Check> checks() { return this.checks; }

   public final Set<Fact> query(final Predicate pred) {
      return this.facts.stream().filter((f) -> {
         if (f.predicate().name() != pred.name()) {
            return false;
         }
         final int min_size = Math.min(f.predicate().terms().size(), pred.terms().size());
         for (int i = 0; i < min_size; ++i) {
            final Term fid = f.predicate().terms().get(i);
            final Term pid = pred.terms().get(i);
            if ((fid instanceof Term.Integer || fid instanceof Term.Str || fid instanceof Term.Date)
                    && fid.getClass() == pid.getClass()) {
               if (!fid.equals(pid)) {
                  return false;
               }
            /* FIXME: is it still necessary?
            } else if (!(fid instanceof Term.Symbol && pid instanceof Term.Variable)) {
               return false;*/
            }
         }
         return true;
      }).collect(Collectors.toSet());
   }

   public final Set<Fact> query_rule(final Rule rule, SymbolTable symbols) {
      final Set<Fact> new_facts = new HashSet<>();
      rule.apply(this.facts, new_facts, symbols);
      return new_facts;
   }

   public final boolean test_rule(final Rule rule, SymbolTable symbols) {
      return rule.test(this.facts, symbols);
   }

   public World() {
      this.facts = new HashSet<>();
      this.rules = new ArrayList<>();
      this.privileged_rules = new ArrayList<>();
      this.checks = new ArrayList<>();
   }

   public World(Set<Fact> facts, List<Rule> privileged_rules,  List<Rule> rules) {
      this.facts = facts;
      this.privileged_rules = privileged_rules;
      this.rules = rules;
      this.checks = new ArrayList<>();
   }

   public World(Set<Fact> facts, List<Rule> privileged_rules, List<Rule> rules, List<Check> checks) {
      this.facts = facts;
      this.privileged_rules = privileged_rules;
      this.rules = rules;
      this.checks = checks;
   }

   public World(World w) {
      this.facts = new HashSet<>();
      for(Fact fact: w.facts) {
         this.facts.add(fact);
      }
      this.privileged_rules = new ArrayList<>();
      for(Rule rule: w.privileged_rules) {
         this.privileged_rules.add(rule);
      }
      this.rules = new ArrayList<>();
      for(Rule rule: w.rules) {
         this.rules.add(rule);
      }
      this.checks = new ArrayList<>();
      for(Check check : w.checks) {
         this.checks.add(check);
      }
   }

   public String print(SymbolTable symbol_table) {
      StringBuilder s = new StringBuilder();

      s.append("World {\n\t\tfacts: [");
      for(Fact f: this.facts) {
         s.append("\n\t\t\t");
         s.append(symbol_table.print_fact(f));
      }
      s.append("\n\t\t]\n\t\tprivileged rules: [");
      for(Rule r: this.privileged_rules) {
         s.append("\n\t\t\t");
         s.append(symbol_table.print_rule(r));
      }
      s.append("\n\t\t]\n\t\trules: [");
      for(Rule r: this.rules) {
         s.append("\n\t\t\t");
         s.append(symbol_table.print_rule(r));
      }
      s.append("\n\t\t]\n\t\tchecks: [");
      for(Check c: this.checks) {
         s.append("\n\t\t\t");
         s.append(symbol_table.print_check(c));
      }
      s.append("\n\t\t]\n\t}");

      return s.toString();
   }
}
