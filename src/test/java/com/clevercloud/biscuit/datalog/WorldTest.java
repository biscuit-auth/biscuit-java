package com.clevercloud.biscuit.datalog;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.ArrayList;
import java.util.Arrays;

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
   }
}
