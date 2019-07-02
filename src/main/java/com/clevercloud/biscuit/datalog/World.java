package com.clevercloud.biscuit.datalog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class World implements Serializable {
   private final Set<Fact> facts;
   private final List<Rule> rules;

   public void add_fact(final Fact fact) {
      this.facts.add(fact);
   }

   public void add_rule(final Rule rule) {
      this.rules.add(rule);
   }

   public void run() {
      for (int i = 0; i < 100; ++i) {
         final Set<Fact> new_facts = new HashSet<>();
         for (final Rule rule : this.rules) {
            rule.apply(this.facts, new_facts);
         }
         final int len = this.facts.size();
         this.facts.addAll(new_facts);
         if (this.facts.size() == len) {
            return;
         }
      }
      assert "world ran more than 100 iterations" == null;
   }

   public Set<Fact> query_rule(final Rule rule) {
      final Set<Fact> new_facts = new HashSet<>();
      rule.apply(this.facts, new_facts);
      return new_facts;
   }

   public World() {
      this.facts = new HashSet<>();
      this.rules = new ArrayList<>();
   }
}
