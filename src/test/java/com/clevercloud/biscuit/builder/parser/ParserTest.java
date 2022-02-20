package com.clevercloud.biscuit.builder.parser;

import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.datalog.expressions.Op;
import com.clevercloud.biscuit.token.builder.*;
import com.clevercloud.biscuit.token.builder.parser.Error;
import com.clevercloud.biscuit.token.builder.parser.Parser;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Option;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static com.clevercloud.biscuit.token.builder.Utils.*;

public class ParserTest extends TestCase {
    public ParserTest(String testName) { super(testName); }

    public static Test suite() {
        return new TestSuite(com.clevercloud.biscuit.builder.parser.ParserTest.class);
    }

    public void testName() {
        Either<Error, Tuple2<String, String>> res = Parser.name("operation(read)");
        assertEquals(Either.right(new Tuple2<String, String>("(read)", "operation")), res);
    }

    public void testString() {
        Either<Error, Tuple2<String, Term.Str>> res = Parser.string("\"file1 a hello - 123_\"");
        assertEquals(Either.right(new Tuple2<String, Term.Str>("", (Term.Str) string("file1 a hello - 123_"))), res);
    }

    public void testInteger() {
        Either<Error, Tuple2<String, Term.Integer>> res = Parser.integer("123");
        assertEquals(Either.right(new Tuple2<String, Term.Integer>("", (Term.Integer) integer(123))), res);

        Either<Error, Tuple2<String, Term.Integer>> res2 = Parser.integer("-42");
        assertEquals(Either.right(new Tuple2<String, Term.Integer>("", (Term.Integer) integer(-42))), res2);
    }

    public void testDate() {
        Either<Error, Tuple2<String, Term.Date>> res = Parser.date("2019-12-02T13:49:53Z,");
        assertEquals(Either.right(new Tuple2<String, Term.Date>(",", new Term.Date(1575294593))), res);
    }

    public void testVariable() {
        Either<Error, Tuple2<String, Term.Variable>> res = Parser.variable("$name");
        assertEquals(Either.right(new Tuple2<String, Term.Variable>("", (Term.Variable) var("name"))), res);
    }

    public void testConstraint() {
    }

    public void testFact() throws com.clevercloud.biscuit.error.Error.Language {
        Either<Error, Tuple2<String, Fact>> res = Parser.fact("right( \"file1\", \"read\" )");
        assertEquals(Either.right(new Tuple2<String, Fact>("",
                fact("right", Arrays.asList(string("file1"), s("read"))))),
                res);

        Either<Error, Tuple2<String, Fact>> res2 = Parser.fact("right( $var, \"read\" )");
        //assertEquals(Either.left(new Error("$var, #read )", "variables are not allowed in facts")),
        //    res2);
        assertEquals(Either.left(new Error("$var, \"read\" )", "closing parens not found")),
                res2);

        Either<Error, Tuple2<String, Fact>> res3 = Parser.fact("date(2019-12-02T13:49:53Z)");
        assertEquals(Either.right(new Tuple2<String, Fact>("",
                        fact("date", Arrays.asList(new Term.Date(1575294593))))),
                res3);
    }

    public void testRule() {
        Either<Error, Tuple2<String, Rule>> res =
                Parser.rule("right($resource, \"read\") <- resource($resource), operation(\"read\")");
        assertEquals(Either.right(new Tuple2<String, Rule>("",
                        rule("right",
                                Arrays.asList(var("resource"), s("read")),
                                Arrays.asList(
                                        pred("resource", Arrays.asList(var("resource"))),
                                        pred("operation", Arrays.asList(s("read"))))
                        ))),
                res);
    }

    public void testRuleWithExpression() {
            Either<Error, Tuple2<String, Rule>> res =
                Parser.rule("valid_date(\"file1\") <- time($0 ), resource( \"file1\"), $0 <= 2019-12-04T09:46:41+00:00");
        assertEquals(Either.right(new Tuple2<String, Rule>("",
                        constrained_rule("valid_date",
                                Arrays.asList(string("file1")),
                                Arrays.asList(
                                        pred("time", Arrays.asList(var("0"))),
                                        pred("resource", Arrays.asList( string("file1")))
                                        ),
                                Arrays.asList(
                                        new Expression.Binary(
                                                Expression.Op.LessOrEqual,
                                                new Expression.Value(var("0")),
                                                new Expression.Value(new Term.Date(1575452801)))
                                )
                        ))),
                res);
    }

    public void testRuleWithExpressionOrdering() {
        Either<Error, Tuple2<String, Rule>> res =
                Parser.rule("valid_date(\"file1\") <- time($0 ), $0 <= 2019-12-04T09:46:41+00:00, resource(\"file1\")");
        assertEquals(Either.right(new Tuple2<String, Rule>("",
                        constrained_rule("valid_date",
                                Arrays.asList(string("file1")),
                                Arrays.asList(
                                        pred("time", Arrays.asList(var("0"))),
                                        pred("resource", Arrays.asList(string("file1")))
                                ),
                                Arrays.asList(
                                        new Expression.Binary(
                                                Expression.Op.LessOrEqual,
                                                new Expression.Value(var("0")),
                                                new Expression.Value(new Term.Date(1575452801)))
                                )
                        ))),
                res);
    }

    public void testCheck() {
        Either<Error, Tuple2<String, Check>> res =
                Parser.check("check if resource($0), operation(\"read\") or admin()");
        assertEquals(Either.right(new Tuple2<String, Check>("", new Check(Arrays.asList(
                    rule("query",
                            new ArrayList<>(),
                            Arrays.asList(
                                    pred("resource", Arrays.asList(var("0"))),
                                    pred("operation", Arrays.asList(s("read")))
                            )
                    ),
                    rule("query",
                            new ArrayList<>(),
                            Arrays.asList(
                                    pred("admin", Arrays.asList())
                            )
                    )
                    )))),
                res);
    }

    public void testExpression() {
        Either<Error, Tuple2<String, Expression>> res =
                Parser.expression(" -1 ");

        assertEquals(new Tuple2<String, Expression>("",
                new Expression.Value(integer(-1))),
                res.get());

        Either<Error, Tuple2<String, Expression>> res2 =
                Parser.expression(" $0 <= 2019-12-04T09:46:41+00:00");

        assertEquals(new Tuple2<String, Expression>("",
                        new Expression.Binary(
                                Expression.Op.LessOrEqual,
                                new Expression.Value(var("0")),
                                new Expression.Value(new Term.Date(1575452801)))),
                res2.get());

        Either<Error, Tuple2<String, Expression>> res3 =
                Parser.expression(" 1 < $test + 2 ");

        assertEquals(Either.right(new Tuple2<String, Expression>("",
                        new Expression.Binary(
                                Expression.Op.LessThan,
                                new Expression.Value(integer(1)),
                                new Expression.Binary(
                                        Expression.Op.Add,
                                        new Expression.Value(var("test")),
                                        new Expression.Value(integer(2))
                                )
                        )
                )),
                res3);

        SymbolTable s3 = new SymbolTable();
        long test = s3.insert("test");
        assertEquals(
                Arrays.asList(
                        new Op.Value(new com.clevercloud.biscuit.datalog.Term.Integer(1)),
                        new Op.Value(new com.clevercloud.biscuit.datalog.Term.Variable(test)),
                        new Op.Value(new com.clevercloud.biscuit.datalog.Term.Integer(2)),
                        new Op.Binary(Op.BinaryOp.Add),
                        new Op.Binary(Op.BinaryOp.LessThan)
                ),
                res3.get()._2.convert(s3).getOps()
        );

        Either<Error, Tuple2<String, Expression>> res4 =
                Parser.expression("  2 < $test && $var2.starts_with(\"test\") && true ");

        assertEquals(Either.right(new Tuple2<String, Expression>("",
                        new Expression.Binary(
                                Expression.Op.And,
                                new Expression.Binary(
                                        Expression.Op.And,
                                        new Expression.Binary(
                                                Expression.Op.LessThan,
                                                new Expression.Value(integer(2)),
                                                new Expression.Value(var("test"))
                                        ),
                                        new Expression.Binary(
                                                Expression.Op.Prefix,
                                                new Expression.Value(var("var2")),
                                                new Expression.Value(string("test"))
                                        )
                                ),
                                new Expression.Value(new Term.Bool(true))
                        )
                )),
                res4);

        Either<Error, Tuple2<String, Expression>> res5 =
                Parser.expression("  [ \"abc\", \"def\" ].contains($operation) ");

        HashSet<Term> s = new HashSet();
        s.add(s("abc"));
        s.add(s("def"));

        assertEquals(Either.right(new Tuple2<String, Expression>("",
                        new Expression.Binary(
                                Expression.Op.Contains,
                                new Expression.Value(set(s)),
                                new Expression.Value(var("operation"))
                        )
                )),
                res5);
    }

    public void testParens() {
        Either<Error, Tuple2<String, Expression>> res =
                Parser.expression("  1 + 2 * 3  ");

        assertEquals(Either.right(new Tuple2<String, Expression>("",
                        new Expression.Binary(
                                Expression.Op.Add,
                                new Expression.Value(integer(1)),
                                new Expression.Binary(
                                        Expression.Op.Mul,
                                        new Expression.Value(integer(2)),
                                        new Expression.Value(integer(3))
                                )
                        )
                )),
                res);

        Expression e = res.get()._2;
        SymbolTable s = new SymbolTable();

        com.clevercloud.biscuit.datalog.expressions.Expression ex = e.convert(s);

        assertEquals(
                Arrays.asList(
                        new Op.Value(new com.clevercloud.biscuit.datalog.Term.Integer(1)),
                        new Op.Value(new com.clevercloud.biscuit.datalog.Term.Integer(2)),
                        new Op.Value(new com.clevercloud.biscuit.datalog.Term.Integer(3)),
                        new Op.Binary(Op.BinaryOp.Mul),
                        new Op.Binary(Op.BinaryOp.Add)
                ),
                ex.getOps()
        );

        HashMap variables = new HashMap();
        Option<com.clevercloud.biscuit.datalog.Term> value = ex.evaluate(variables, s);
        assertEquals(Option.some(new com.clevercloud.biscuit.datalog.Term.Integer(7)), value);
        assertEquals("1 + 2 * 3", ex.print(s).get());


        Either<Error, Tuple2<String, Expression>> res2 =
                Parser.expression("  (1 + 2) * 3  ");

        assertEquals(Either.right(new Tuple2<String, Expression>("",
                        new Expression.Binary(
                                Expression.Op.Mul,
                                new Expression.Unary(
                                        Expression.Op.Parens,
                                        new Expression.Binary(
                                                Expression.Op.Add,
                                                new Expression.Value(integer(1)),
                                                new Expression.Value(integer(2))
                                        ))
                                ,
                                new Expression.Value(integer(3))
                        )
                )),
                res2);

        Expression e2 = res2.get()._2;
        SymbolTable s2 = new SymbolTable();

        com.clevercloud.biscuit.datalog.expressions.Expression ex2 = e2.convert(s2);

        assertEquals(
                Arrays.asList(
                        new Op.Value(new com.clevercloud.biscuit.datalog.Term.Integer(1)),
                        new Op.Value(new com.clevercloud.biscuit.datalog.Term.Integer(2)),
                        new Op.Binary(Op.BinaryOp.Add),
                        new Op.Unary(Op.UnaryOp.Parens),
                        new Op.Value(new com.clevercloud.biscuit.datalog.Term.Integer(3)),
                        new Op.Binary(Op.BinaryOp.Mul)
                ),
                ex2.getOps()
        );

        HashMap variables2 = new HashMap();
        Option<com.clevercloud.biscuit.datalog.Term> value2 = ex2.evaluate(variables2, s2);
        assertEquals(Option.some(new com.clevercloud.biscuit.datalog.Term.Integer(9)), value2);
        assertEquals("(1 + 2) * 3", ex2.print(s2).get());
    }
}