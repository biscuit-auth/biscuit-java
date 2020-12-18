package com.clevercloud.biscuit.builder.parser;

import com.clevercloud.biscuit.token.builder.Term;
import com.clevercloud.biscuit.token.builder.Fact;
import com.clevercloud.biscuit.token.builder.Rule;
import com.clevercloud.biscuit.token.builder.parser.Error;
import com.clevercloud.biscuit.token.builder.parser.Parser;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Arrays;

import static com.clevercloud.biscuit.token.builder.Utils.*;

public class ParserTest extends TestCase {
    public ParserTest(String testName) { super(testName); }

    public static Test suite() {
        return new TestSuite(com.clevercloud.biscuit.builder.parser.ParserTest.class);
    }

    public void testName() {
        Either<Error, Tuple2<String, String>> res = Parser.name("operation(#ambient, #read)");
        assertEquals(Either.right(new Tuple2<String, String>("(#ambient, #read)", "operation")), res);
    }

    public void testSymbol() {
        Either<Error, Tuple2<String, Term.Symbol>> res = Parser.symbol("#ambient");
        assertEquals(Either.right(new Tuple2<String, Term.Symbol>("", (Term.Symbol) s("ambient"))), res);
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

    public void testFact() {
        Either<Error, Tuple2<String, Fact>> res = Parser.fact("right( #authority, \"file1\", #read )");
        assertEquals(Either.right(new Tuple2<String, Fact>("",
                fact("right", Arrays.asList(s("authority"), string("file1"), s("read"))))),
                res);
    }

    public void testRule() {
        Either<Error, Tuple2<String, Rule>> res = Parser.rule("*right(#authority, $resource, #read) <- resource( #ambient, $resource), operation(#ambient, #read)");
        assertEquals(Either.right(new Tuple2<String, Rule>("",
                rule("right",
                        Arrays.asList(s("authority"), var("resource"), s("read")),
                        Arrays.asList(
                                pred("resource", Arrays.asList(s("ambient"),  var("resource"))),
                                pred("operation", Arrays.asList(s("ambient"), s("read"))))
                ))),
                res);
    }
}