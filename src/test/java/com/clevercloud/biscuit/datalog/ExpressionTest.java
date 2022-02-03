package com.clevercloud.biscuit.datalog;

import com.clevercloud.biscuit.datalog.expressions.Expression;
import com.clevercloud.biscuit.datalog.expressions.Op;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class ExpressionTest extends TestCase {
    public ExpressionTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(ExpressionTest.class);
    }

    public void testNegate() {
        SymbolTable symbols = new SymbolTable();
        symbols.add("a");
        symbols.add("b");
        symbols.add("var");


        Expression e = new Expression(new ArrayList<Op>(Arrays.asList(
                new Op.Value(new Term.Integer(1)),
                new Op.Value(new Term.Variable(2)),
                new Op.Binary(Op.BinaryOp.LessThan),
                new Op.Unary(Op.UnaryOp.Negate)
        )));

        Assert.assertEquals(
                "! 1 < $var",
                e.print(symbols).get()
        );

        HashMap<Long, Term> variables = new HashMap<>();
        variables.put(2L, new Term.Integer(0));

        Assert.assertEquals(
                new Term.Bool(true),
                e.evaluate(variables, symbols).get()
        );
    }
}
