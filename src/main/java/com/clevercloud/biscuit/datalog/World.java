package com.clevercloud.biscuit.datalog;

import com.clevercloud.biscuit.error.Error;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class World implements Serializable {
   private final Set<Fact> facts;
   private final List<Rule> rules;

   public void add_fact(final Fact fact) {
      this.facts.add(fact);
   }

   public void add_facts(final Set<Fact> facts) {
      this.facts.addAll(facts);
   }

   public void add_rule(final Rule rule) {
      this.rules.add(rule);
   }

   public void clearRules() {
      this.rules.clear();
   }

   public void run(final SymbolTable symbols) throws Error.TooManyFacts, Error.TooManyIterations, Error.Timeout {
      this.run(new RunLimits(), symbols);
   }

   public void run(RunLimits limits, final SymbolTable symbols) throws Error.TooManyFacts, Error.TooManyIterations, Error.Timeout {
      int iterations = 0;
      Instant limit = Instant.now().plus(limits.maxTime);

      while(true) {
         final Set<Fact> new_facts = new HashSet<>();

         for (final Rule rule : this.rules) {
            rule.apply(this.facts, new_facts, symbols);

            if(Instant.now().compareTo(limit) >= 0) {
               throw new Error.Timeout();
            }
         }

         final int len = this.facts.size();
         this.facts.addAll(new_facts);
         if (this.facts.size() == len) {
            return ;
         }

         if (this.facts.size() >= limits.maxFacts) {
            throw new Error.TooManyFacts();
         }

         iterations += 1;
         if(iterations >= limits.maxIterations) {
            throw new Error.TooManyIterations();
         }
      }
   }

   public final Set<Fact> facts() {
      return this.facts;
   }

   public List<Rule> rules() { return this.rules; }

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

   public final boolean query_match(final Rule rule, SymbolTable symbols) {
      return rule.find_match(this.facts, symbols);
   }

   public final boolean query_match_all(final Rule rule, SymbolTable symbols) {
      return rule.check_match_all(this.facts, symbols);
   }


   public World() {
      this.facts = new HashSet<>();
      this.rules = new ArrayList<>();
   }

   public World(Set<Fact> facts) {
      this.facts = new HashSet<>();
      this.facts.addAll(facts);
      this.rules = new ArrayList<>();
   }

   public World(Set<Fact> facts,  List<Rule> rules) {
      this.facts = facts;
      this.rules = rules;
   }

   public World(Set<Fact> facts, List<Rule> rules, List<Check> checks) {
      this.facts = facts;
      this.rules = rules;
   }

   public World(World w) {
      this.facts = new HashSet<>();
      for(Fact fact: w.facts) {
         this.facts.add(fact);
      }

      this.rules = new ArrayList<>();
      for(Rule rule: w.rules) {
         this.rules.add(rule);
      }

   }

   public String print(SymbolTable symbol_table) {
      StringBuilder s = new StringBuilder();

      s.append("World {\n\t\tfacts: [");
      for(Fact f: this.facts) {
         s.append("\n\t\t\t");
         s.append(symbol_table.print_fact(f));
      }

      s.append("\n\t\t]\n\t\trules: [");
      for(Rule r: this.rules) {
         s.append("\n\t\t\t");
         s.append(symbol_table.print_rule(r));
      }

      s.append("\n\t\t]\n\t}");

      return s.toString();
   }
}
