package com.clevercloud.biscuit.datalog;

import com.clevercloud.biscuit.datalog.constraints.Constraint;
import com.clevercloud.biscuit.datalog.constraints.ConstraintKind;
import com.clevercloud.biscuit.datalog.constraints.IntConstraint;
import com.clevercloud.biscuit.datalog.constraints.StrConstraint;
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
      final long sibling = syms.insert("syblings");

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

      w.add_rule(new Rule(new Predicate(sibling, Arrays.asList(new ID.Variable("sibling1"), new ID.Variable("sibling2"))), Arrays.asList(
            new Predicate(parent, Arrays.asList(new ID.Variable("parent"), new ID.Variable("sibling1"))),
            new Predicate(parent, Arrays.asList(new ID.Variable("parent"), new ID.Variable("sibling2")))
      ), new ArrayList<>()));
      w.run();

      System.out.println("siblings: [" + String.join(", ", w.query(new Predicate(sibling, Arrays.asList(new ID.Variable("sibling1"), new ID.Variable("sibling2")))).stream().map((f) -> syms.print_fact(f)).collect(Collectors.toSet())) + "]");
   }

   public void testNumbers() {
      final World w = new World();
      final SymbolTable syms = new SymbolTable();

      final ID abc = syms.add("abc");
      final ID def = syms.add("def");
      final ID ghi = syms.add("ghi");
      final ID jkl = syms.add("jkl");
      final ID mno = syms.add("mno");
      final ID aaa = syms.add("AAA");
      final ID bbb = syms.add("BBB");
      final ID ccc = syms.add("CCC");
      final long t1 = syms.insert("t1");
      final long t2 = syms.insert("t2");
      final long join = syms.insert("join");

      w.add_fact(new Fact(new Predicate(t1, Arrays.asList(new ID.Integer(0), abc))));
      w.add_fact(new Fact(new Predicate(t1, Arrays.asList(new ID.Integer(1), def))));
      w.add_fact(new Fact(new Predicate(t1, Arrays.asList(new ID.Integer(2), ghi))));
      w.add_fact(new Fact(new Predicate(t1, Arrays.asList(new ID.Integer(3), jkl))));
      w.add_fact(new Fact(new Predicate(t1, Arrays.asList(new ID.Integer(4), mno))));

      w.add_fact(new Fact(new Predicate(t2, Arrays.asList(new ID.Integer(0), aaa, new ID.Integer(0)))));
      w.add_fact(new Fact(new Predicate(t2, Arrays.asList(new ID.Integer(1), bbb, new ID.Integer(0)))));
      w.add_fact(new Fact(new Predicate(t2, Arrays.asList(new ID.Integer(2), ccc, new ID.Integer(1)))));

      Set<Fact> res = w.query_rule(new Rule(new Predicate(join, Arrays.asList(new ID.Variable("left"), new ID.Variable("right"))), Arrays.asList(new Predicate(t1, Arrays.asList(new ID.Variable("id"), new ID.Variable("left"))), new Predicate(t2, Arrays.asList(new ID.Variable("t2_id"), new ID.Variable("right"), new ID.Variable("id")))), new ArrayList<>()));
      for (final Fact f : res) {
         System.out.println("\t" + syms.print_fact(f));
      }
      Set<Fact> expected = new HashSet<>(Arrays.asList(new Fact(new Predicate(join, Arrays.asList(abc, aaa))), new Fact(new Predicate(join, Arrays.asList(abc, bbb))), new Fact(new Predicate(join, Arrays.asList(def, ccc)))));
      Assert.assertEquals(expected, res);

      res = w.query_rule(new Rule(new Predicate(join, Arrays.asList(new ID.Variable("left"), new ID.Variable("right"))), Arrays.asList(new Predicate(t1, Arrays.asList(new ID.Variable(1234), new ID.Variable("left"))), new Predicate(t2, Arrays.asList(new ID.Variable("t2_id"), new ID.Variable("right"), new ID.Variable(1234)))), Arrays.asList(new Constraint(1234, new ConstraintKind.Int(new IntConstraint.Lower(1))))));
      for (final Fact f : res) {
         System.out.println("\t" + syms.print_fact(f));
      }
      expected = new HashSet<>(Arrays.asList(new Fact(new Predicate(join, Arrays.asList(abc, aaa))), new Fact(new Predicate(join, Arrays.asList(abc, bbb)))));
      Assert.assertEquals(expected, res);
   }

   private final Set<Fact> testSuffix(final World w, final long suff, final long route, final String suffix) {
      return w.query_rule(new Rule(new Predicate(suff, Arrays.asList(new ID.Variable("app_id"), new ID.Variable(1234))), Arrays.asList(
            new Predicate(route, Arrays.asList(new ID.Variable(0), new ID.Variable("app_id"), new ID.Variable(1234)))
      ), Arrays.asList(
            new Constraint(1234, new ConstraintKind.Str(new StrConstraint.Suffix(suffix)))
      )));
   }

   public void testStr() {
      final World w = new World();
      final SymbolTable syms = new SymbolTable();

      final ID app_0 = syms.add("app_0");
      final ID app_1 = syms.add("app_1");
      final ID app_2 = syms.add("app_2");
      final long route = syms.insert("route");
      final long suff = syms.insert("route suffix");

      w.add_fact(new Fact(new Predicate(route, Arrays.asList(new ID.Integer(0), app_0, new ID.Str("example.com")))));
      w.add_fact(new Fact(new Predicate(route, Arrays.asList(new ID.Integer(1), app_1, new ID.Str("test.com")))));
      w.add_fact(new Fact(new Predicate(route, Arrays.asList(new ID.Integer(2), app_2, new ID.Str("test.fr")))));
      w.add_fact(new Fact(new Predicate(route, Arrays.asList(new ID.Integer(3), app_0, new ID.Str("www.example.com")))));
      w.add_fact(new Fact(new Predicate(route, Arrays.asList(new ID.Integer(4), app_1, new ID.Str("mx.example.com")))));

      Set<Fact> res = testSuffix(w, suff, route, ".fr");
      for (final Fact f : res) {
         System.out.println("\t" + syms.print_fact(f));
      }
      Set<Fact> expected = new HashSet<>(Arrays.asList(new Fact(new Predicate(suff, Arrays.asList(app_2, new ID.Str("test.fr"))))));
      Assert.assertEquals(expected, res);

      res = testSuffix(w, suff, route, "example.com");
      for (final Fact f : res) {
         System.out.println("\t" + syms.print_fact(f));
      }
      expected = new HashSet<>(Arrays.asList(new Fact(new Predicate(suff, Arrays.asList(app_0, new ID.Str("example.com")))), new Fact(new Predicate(suff, Arrays.asList(app_0, new ID.Str("www.example.com")))), new Fact(new Predicate(suff, Arrays.asList(app_1, new ID.Str("mx.example.com"))))));
      Assert.assertEquals(expected, res);
   }
}
