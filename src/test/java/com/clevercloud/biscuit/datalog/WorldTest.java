package com.clevercloud.biscuit.datalog;

import com.clevercloud.biscuit.datalog.constraints.*;
import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.time.Instant;
import java.util.*;
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

      final Rule r1 = new Rule(new Predicate(grandparent,
              Arrays.asList(new ID.Variable(syms.insert("grandparent")), new ID.Variable(syms.insert("grandchild")))), Arrays.asList(
            new Predicate(parent, Arrays.asList(new ID.Variable(syms.insert("grandparent")), new ID.Variable(syms.insert("parent")))),
            new Predicate(parent, Arrays.asList(new ID.Variable(syms.insert("parent")), new ID.Variable(syms.insert("grandchild"))))
      ), new ArrayList<>());

      System.out.println("testing r1: " + syms.print_rule(r1));
      Set<Fact> query_rule_result = w.query_rule(r1);
      System.out.println("grandparents query_rules: [" + String.join(", ", query_rule_result.stream().map((f) -> syms.print_fact(f)).collect(Collectors.toList())) + "]");
      System.out.println("current facts: [" + String.join(", ", w.facts().stream().map((f) -> syms.print_fact(f)).collect(Collectors.toList())) + "]");

      final Rule r2 = new Rule(new Predicate(grandparent,
              Arrays.asList(new ID.Variable(syms.insert("grandparent")), new ID.Variable(syms.insert("grandchild")))), Arrays.asList(
            new Predicate(parent, Arrays.asList(new ID.Variable(syms.insert("grandparent")), new ID.Variable(syms.insert("parent")))),
            new Predicate(parent, Arrays.asList(new ID.Variable(syms.insert("parent")), new ID.Variable(syms.insert("grandchild"))))
      ), new ArrayList<>());

      System.out.println("adding r2: " + syms.print_rule(r2));
      w.add_rule(r2);
      w.run();

      System.out.println("parents:");
      for (final Fact fact : w.query(new Predicate(parent,
              Arrays.asList(new ID.Variable(syms.insert("parent")), new ID.Variable(syms.insert("child")))))) {
         System.out.println("\t" + syms.print_fact(fact));
      }
      System.out.println("parents of B: [" + String.join(", ",
              w.query(new Predicate(parent, Arrays.asList(new ID.Variable(syms.insert("parent")), b)))
                      .stream().map((f) -> syms.print_fact(f)).collect(Collectors.toSet())) + "]");
      System.out.println("grandparents: [" + String.join(", ",
              w.query(new Predicate(grandparent, Arrays.asList(new ID.Variable(syms.insert("grandparent")),
                      new ID.Variable(syms.insert("grandchild")))))
                      .stream().map((f) -> syms.print_fact(f)).collect(Collectors.toSet())) + "]");

      w.add_fact(new Fact(new Predicate(parent, Arrays.asList(c, e))));
      w.run();

      final Set<Fact> res = w.query(new Predicate(grandparent,
              Arrays.asList(new ID.Variable(syms.insert("grandparent")), new ID.Variable(syms.insert("grandchild")))));
      System.out.println("grandparents after inserting parent(C, E): [" + String.join(", ",
              res.stream().map((f) -> syms.print_fact(f)).collect(Collectors.toSet())) + "]");

      final Set<Fact> expected = new HashSet<>(Arrays.asList(
              new Fact(new Predicate(grandparent, Arrays.asList(a, c))),
              new Fact(new Predicate(grandparent, Arrays.asList(b, d))),
              new Fact(new Predicate(grandparent, Arrays.asList(b, e)))));
      Assert.assertEquals(expected, res);

      w.add_rule(new Rule(new Predicate(sibling,
              Arrays.asList(new ID.Variable(syms.insert("sibling1")), new ID.Variable(syms.insert("sibling2")))), Arrays.asList(
            new Predicate(parent, Arrays.asList(new ID.Variable(syms.insert("parent")), new ID.Variable(syms.insert("sibling1")))),
            new Predicate(parent, Arrays.asList(new ID.Variable(syms.insert("parent")), new ID.Variable(syms.insert("sibling2"))))
      ), new ArrayList<>()));
      w.run();

      System.out.println("siblings: [" + String.join(", ",
              w.query(new Predicate(sibling, Arrays.asList(
                      new ID.Variable(syms.insert("sibling1")),
                      new ID.Variable(syms.insert("sibling2")))))
                      .stream().map((f) -> syms.print_fact(f)).collect(Collectors.toSet())) + "]");
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

      Set<Fact> res = w.query_rule(new Rule(new Predicate(join,
              Arrays.asList(new ID.Variable(syms.insert("left")), new ID.Variable(syms.insert("right")))
            ),
            Arrays.asList(new Predicate(t1, Arrays.asList(new ID.Variable(syms.insert("id")), new ID.Variable(syms.insert("left")))),
                    new Predicate(t2,
                            Arrays.asList(
                                    new ID.Variable(syms.insert("t2_id")),
                                    new ID.Variable(syms.insert("right")),
                                    new ID.Variable(syms.insert("id"))))), new ArrayList<>()));
      for (final Fact f : res) {
         System.out.println("\t" + syms.print_fact(f));
      }
      Set<Fact> expected = new HashSet<>(Arrays.asList(new Fact(new Predicate(join, Arrays.asList(abc, aaa))), new Fact(new Predicate(join, Arrays.asList(abc, bbb))), new Fact(new Predicate(join, Arrays.asList(def, ccc)))));
      Assert.assertEquals(expected, res);

      res = w.query_rule(new Rule(new Predicate(join,
              Arrays.asList(new ID.Variable(syms.insert("left")), new ID.Variable(syms.insert("right")))),
              Arrays.asList(new Predicate(t1, Arrays.asList(new ID.Variable(syms.insert("id")), new ID.Variable(syms.insert("left")))),
                      new Predicate(t2,
                              Arrays.asList(
                                      new ID.Variable(syms.insert("t2_id")),
                                      new ID.Variable(syms.insert("right")),
                                      new ID.Variable(syms.insert("id"))))),
              Arrays.asList(new Constraint(syms.insert("id"), new ConstraintKind.Int(new IntConstraint.LessThan(1))))));
      for (final Fact f : res) {
         System.out.println("\t" + syms.print_fact(f));
      }
      expected = new HashSet<>(Arrays.asList(new Fact(new Predicate(join, Arrays.asList(abc, aaa))), new Fact(new Predicate(join, Arrays.asList(abc, bbb)))));
      Assert.assertEquals(expected, res);
   }

   private final Set<Fact> testSuffix(final World w, SymbolTable syms, final long suff, final long route, final String suffix) {
      return w.query_rule(new Rule(new Predicate(suff,
              Arrays.asList(new ID.Variable(syms.insert("app_id")), new ID.Variable(syms.insert("domain")))),
              Arrays.asList(
            new Predicate(route, Arrays.asList(
                    new ID.Variable(syms.insert("route_id")),
                    new ID.Variable(syms.insert("app_id")),
                    new ID.Variable(syms.insert("domain"))))
      ), Arrays.asList(
            new Constraint(syms.insert("domain"), new ConstraintKind.Str(new StrConstraint.Suffix(suffix)))
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

      Set<Fact> res = testSuffix(w, syms, suff, route, ".fr");
      for (final Fact f : res) {
         System.out.println("\t" + syms.print_fact(f));
      }
      Set<Fact> expected = new HashSet<>(Arrays.asList(new Fact(new Predicate(suff, Arrays.asList(app_2, new ID.Str("test.fr"))))));
      Assert.assertEquals(expected, res);

      res = testSuffix(w, syms, suff, route, "example.com");
      for (final Fact f : res) {
         System.out.println("\t" + syms.print_fact(f));
      }
      expected = new HashSet<>(Arrays.asList(new Fact(new Predicate(suff,
              Arrays.asList(
                      app_0,
                      new ID.Str("example.com")))),
              new Fact(new Predicate(suff,
                      Arrays.asList(app_0, new ID.Str("www.example.com")))),
              new Fact(new Predicate(suff, Arrays.asList(app_1, new ID.Str("mx.example.com"))))));
      Assert.assertEquals(expected, res);
   }

   public void testDate() {
      final World w = new World();
      final SymbolTable syms = new SymbolTable();

      final Instant t1 = new Date().toInstant();
      System.out.println("t1 = " + t1);
      final Instant t2 = t1.plusSeconds(10);
      System.out.println("t2 = " + t2);
      final Instant t3 = t2.plusSeconds(30);
      System.out.println("t3 = " + t3);

      final long t2_timestamp = t2.getEpochSecond();

      final ID abc = syms.add("abc");
      final ID def = syms.add("def");
      final long x = syms.insert("x");
      final long before = syms.insert("before");
      final long after = syms.insert("after");

      w.add_fact(new Fact(new Predicate(x, Arrays.asList(new ID.Date(t1.getEpochSecond()), abc))));
      w.add_fact(new Fact(new Predicate(x, Arrays.asList(new ID.Date(t3.getEpochSecond()), def))));

      final Rule r1 = new Rule(new Predicate(before, Arrays.asList(new ID.Variable(syms.insert("date")), new ID.Variable(syms.insert("val")))), Arrays.asList(
            new Predicate(x, Arrays.asList(new ID.Variable(syms.insert("date")), new ID.Variable(syms.insert("val"))))
      ), Arrays.asList(
            new Constraint(syms.insert("date"), new ConstraintKind.Date(new DateConstraint.Before(t2_timestamp))),
            new Constraint(syms.insert("date"), new ConstraintKind.Date(new DateConstraint.After(0)))
      ));

      System.out.println("testing r1: " + syms.print_rule(r1));
      Set<Fact> res = w.query_rule(r1);
      for (final Fact f : res) {
         System.out.println("\t" + syms.print_fact(f));
      }
      Set<Fact> expected = new HashSet<>(Arrays.asList(new Fact(new Predicate(before, Arrays.asList(new ID.Date(t1.getEpochSecond()), abc)))));
      Assert.assertEquals(expected, res);

      final Rule r2 = new Rule(new Predicate(after, Arrays.asList(new ID.Variable(syms.insert("date")), new ID.Variable(syms.insert("val")))), Arrays.asList(
            new Predicate(x, Arrays.asList(new ID.Variable(syms.insert("date")), new ID.Variable(syms.insert("val"))))
      ), Arrays.asList(
            new Constraint(syms.insert("date"), new ConstraintKind.Date(new DateConstraint.After(t2_timestamp))),
            new Constraint(syms.insert("date"), new ConstraintKind.Date(new DateConstraint.After(0)))
      ));

      System.out.println("testing r2: " + syms.print_rule(r2));
      res = w.query_rule(r2);
      for (final Fact f : res) {
         System.out.println("\t" + syms.print_fact(f));
      }
      expected = new HashSet<>(Arrays.asList(new Fact(new Predicate(after, Arrays.asList(new ID.Date(t3.getEpochSecond()), def)))));
      Assert.assertEquals(expected, res);
   }

   public void testSet() {
      final World w = new World();
      final SymbolTable syms = new SymbolTable();

      final ID abc = syms.add("abc");
      final ID def = syms.add("def");
      final long x = syms.insert("x");
      final long int_set = syms.insert("int_set");
      final long symbol_set = syms.insert("symbol_set");
      final long string_set = syms.insert("string_set");

      w.add_fact(new Fact(new Predicate(x, Arrays.asList(abc, new ID.Integer(0), new ID.Str("test")))));
      w.add_fact(new Fact(new Predicate(x, Arrays.asList(def, new ID.Integer(2), new ID.Str("hello")))));

      final Rule r1 = new Rule(new Predicate(int_set, Arrays.asList(new ID.Variable(syms.insert("sym")), new ID.Variable(syms.insert("str")))), Arrays.asList(
            new Predicate(x, Arrays.asList(new ID.Variable(syms.insert("sym")), new ID.Variable(syms.insert("int")), new ID.Variable(syms.insert("str"))))
      ), Arrays.asList(
            new Constraint(syms.insert("int"), new ConstraintKind.Int(new IntConstraint.InSet(new HashSet<>(Arrays.asList(0l, 1l)))))
      ));
      System.out.println("testing r1: " + syms.print_rule(r1));
      Set<Fact> res = w.query_rule(r1);
      for (final Fact f : res) {
         System.out.println("\t" + syms.print_fact(f));
      }
      Set<Fact> expected = new HashSet<>(Arrays.asList(new Fact(new Predicate(int_set, Arrays.asList(abc, new ID.Str("test"))))));
      Assert.assertEquals(expected, res);

      final long abc_sym_id = syms.insert("abc");
      final long ghi_sym_id = syms.insert("ghi");

      final Rule r2 = new Rule(new Predicate(symbol_set, Arrays.asList(new ID.Variable(syms.insert("sym")), new ID.Variable(syms.insert("int")), new ID.Variable(syms.insert("str")))), Arrays.asList(
            new Predicate(x, Arrays.asList(new ID.Variable(syms.insert("sym")), new ID.Variable(syms.insert("int")), new ID.Variable(syms.insert("str"))))
      ), Arrays.asList(
            new Constraint(syms.insert("sym"), new ConstraintKind.Symbol(new SymbolConstraint.NotInSet(new HashSet<>(Arrays.asList(abc_sym_id, ghi_sym_id)))))
      ));
      System.out.println("testing r2: " + syms.print_rule(r2));
      res = w.query_rule(r2);
      for (final Fact f : res) {
         System.out.println("\t" + syms.print_fact(f));
      }
      expected = new HashSet<>(Arrays.asList(new Fact(new Predicate(symbol_set, Arrays.asList(def, new ID.Integer(2), new ID.Str("hello"))))));
      Assert.assertEquals(expected, res);

      final Rule r3 = new Rule(new Predicate(string_set, Arrays.asList(new ID.Variable(syms.insert("sym")), new ID.Variable(syms.insert("int")), new ID.Variable(syms.insert("str")))), Arrays.asList(
            new Predicate(x, Arrays.asList(new ID.Variable(syms.insert("sym")), new ID.Variable(syms.insert("int")), new ID.Variable(syms.insert("str"))))
      ), Arrays.asList(
            new Constraint(syms.insert("str"), new ConstraintKind.Str(new StrConstraint.InSet(new HashSet<>(Arrays.asList("test", "aaa")))))
      ));
      System.out.println("testing r3: " + syms.print_rule(r3));
      res = w.query_rule(r3);
      for (final Fact f : res) {
         System.out.println("\t" + syms.print_fact(f));
      }
      expected = new HashSet<>(Arrays.asList(new Fact(new Predicate(string_set, Arrays.asList(abc, new ID.Integer(0), new ID.Str("test"))))));
      Assert.assertEquals(expected, res);
   }

   public void testResource() {
      final World w = new World();
      final SymbolTable syms = new SymbolTable();

      final ID authority = syms.add("authority");
      final ID ambient = syms.add("ambient");
      final long resource = syms.insert("resource");
      final long operation = syms.insert("operation");
      final long right = syms.insert("right");
      final ID file1 = syms.add("file1");
      final ID file2 = syms.add("file2");
      final ID read = syms.add("read");
      final ID write = syms.add("write");


      w.add_fact(new Fact(new Predicate(resource, Arrays.asList(ambient, file2))));
      w.add_fact(new Fact(new Predicate(operation, Arrays.asList(ambient, write))));
      w.add_fact(new Fact(new Predicate(right, Arrays.asList(authority, file1, read))));
      w.add_fact(new Fact(new Predicate(right, Arrays.asList(authority, file2, read))));
      w.add_fact(new Fact(new Predicate(right, Arrays.asList(authority, file1, write))));

      final long caveat1 = syms.insert("caveat1");
      //r1: caveat2(#file1) <- resource(#ambient, #file1)
      final Rule r1 = new Rule(
              new Predicate(caveat1, Arrays.asList(file1)),
              Arrays.asList(new Predicate(resource, Arrays.asList(ambient, file1))
      ), new ArrayList<>());

      System.out.println("testing caveat 1: " + syms.print_rule(r1));
      Set<Fact>res = w.query_rule(r1);
      for (final Fact f : res) {
         System.out.println("\t" + syms.print_fact(f));
      }
      Assert.assertTrue(res.isEmpty());

      final long caveat2 = syms.insert("caveat2");
      final ID var0 = new ID.Variable(0);
      //r2: caveat1(0?) <- resource(#ambient, 0?) && operation(#ambient, #read) && right(#authority, 0?, #read)
      final Rule r2 = new Rule(
              new Predicate(caveat2, Arrays.asList(var0)),
              Arrays.asList(
                      new Predicate(resource, Arrays.asList(ambient, var0)),
                      new Predicate(operation, Arrays.asList(ambient, read)),
                      new Predicate(right, Arrays.asList(authority, var0, read))
              ), new ArrayList<>());

      System.out.println("testing caveat 2: " + syms.print_rule(r2));
      res = w.query_rule(r2);
      for (final Fact f : res) {
         System.out.println("\t" + syms.print_fact(f));
      }
      Assert.assertTrue(res.isEmpty());
   }
}
