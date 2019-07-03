package com.clevercloud.biscuit.datalog;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class WorldTest extends TestCase {
   public WorldTest(String testName) {
      super(testName);
   }

   public static Test suite() {
      return new TestSuite(WorldTest.class);
   }

   public void testFamily() {
      final World w = new World();
      final SymbolTable syms = new SymbolTable();
      final ID a = syms.add("A");
      final ID b = syms.add("B");
      final ID c = syms.add("C");
      final ID d = syms.add("D");
      final ID e = syms.add("e");
      final long parent = syms.insert("parent");
      final long grandparent = syms.insert("grandparent");

      w.add_fact(new Fact(new Predicate(parent, Arrays.asList(a, b))));
      w.add_fact(new Fact(new Predicate(parent, Arrays.asList(b, c))));
      w.add_fact(new Fact(new Predicate(parent, Arrays.asList(c, d))));

      final Rule r1 = new Rule(new Predicate(grandparent, Arrays.asList(new ID.Variable("grandparent"), new ID.Variable("grandchild"))), Arrays.asList(
            new Predicate(parent, Arrays.asList(new ID.Variable("grandparent"), new ID.Variable("parent"))),
            new Predicate(parent, Arrays.asList(new ID.Variable("parent"), new ID.Variable("grandchild")))
      ), new ArrayList<>());

      System.out.println("testing r1: " + syms.print_rule(r1));
      Set<Fact> query_rule_result = w.query_rule(r1);
      System.out.println("grandparents query_rules: [" + String.join(", ", query_rule_result.stream().map((f) -> syms.print_fact(f)).collect(Collectors.toList())) + "]");
      System.out.println("current facts: [" + String.join(", ", w.facts().stream().map((f) -> syms.print_fact(f)).collect(Collectors.toList())) + "]");

      final Rule r2 = new Rule(new Predicate(grandparent, Arrays.asList(new ID.Variable("grandparent"), new ID.Variable("grandchild"))), Arrays.asList(
            new Predicate(parent, Arrays.asList(new ID.Variable("grandparent"), new ID.Variable("parent"))),
            new Predicate(parent, Arrays.asList(new ID.Variable("parent"), new ID.Variable("grandchild")))
      ), new ArrayList<>());

      System.out.println("adding r2: " + syms.print_rule(r2));
      w.add_rule(r2);
      w.run();

      System.out.println("parents:");
      for (final Fact fact : w.query(new Predicate(parent, Arrays.asList(new ID.Variable("parent"), new ID.Variable("child"))))) {
         System.out.println("\t" + syms.print_fact(fact));
      }
      System.out.println("parents of B: [" + String.join(", ", w.query(new Predicate(parent, Arrays.asList(new ID.Variable("parent"), b))).stream().map((f) -> syms.print_fact(f)).collect(Collectors.toSet())) + "]");
      System.out.println("grandparents: [" + String.join(", ", w.query(new Predicate(grandparent, Arrays.asList(new ID.Variable("grandparent"), new ID.Variable("grandchild")))).stream().map((f) -> syms.print_fact(f)).collect(Collectors.toSet())) + "]");

      w.add_fact(new Fact(new Predicate(parent, Arrays.asList(c, e))));
      w.run();

      final Set<Fact> res = w.query(new Predicate(grandparent, Arrays.asList(new ID.Variable("grandparent"), new ID.Variable("grandchild"))));
      System.out.println("grandparents after inserting parent(C, E): [" + String.join(", ", res.stream().map((f) -> syms.print_fact(f)).collect(Collectors.toSet())) + "]");

      final Set<Fact> expected = new HashSet<>(Arrays.asList(new Fact(new Predicate(grandparent, Arrays.asList(a, c))), new Fact(new Predicate(grandparent, Arrays.asList(b, d))), new Fact(new Predicate(grandparent, Arrays.asList(b, e)))));
      Assert.assertEquals(expected, res);
   }
}
