package org.biscuitsec.biscuit.datalog;

import org.biscuitsec.biscuit.datalog.expressions.Expression;
import org.biscuitsec.biscuit.datalog.expressions.Op;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class ExpressionTest {

    @Test
    public void testNegate() {
        SymbolTable symbols = new SymbolTable();
        symbols.add("a");
        symbols.add("b");
        symbols.add("var");


        Expression e = new Expression(new ArrayList<Op>(Arrays.asList(
                new Op.Value(new Term.Integer(1)),
                new Op.Value(new Term.Variable(SymbolTable.DEFAULT_SYMBOLS_OFFSET + 2)),
                new Op.Binary(Op.BinaryOp.LessThan),
                new Op.Unary(Op.UnaryOp.Negate)
        )));

        assertEquals(
                "!1 < $var",
                e.print(symbols).get()
        );

        HashMap<Long, Term> variables = new HashMap<>();
        variables.put(SymbolTable.DEFAULT_SYMBOLS_OFFSET + 2L, new Term.Integer(0));

        assertEquals(
                new Term.Bool(true),
                e.evaluate(variables, new TemporarySymbolTable(symbols)).get()
        );
    }

    @Test
    public void testAddsStr() {
        SymbolTable symbols = new SymbolTable();
        symbols.add("a");
        symbols.add("b");
        symbols.add("ab");


        Expression e = new Expression(new ArrayList<Op>(Arrays.asList(
                new Op.Value(new Term.Str(SymbolTable.DEFAULT_SYMBOLS_OFFSET)),
                new Op.Value(new Term.Str(SymbolTable.DEFAULT_SYMBOLS_OFFSET + 1)),
                new Op.Binary(Op.BinaryOp.Add)
        )));

        assertEquals(
                "\"a\" + \"b\"",
                e.print(symbols).get()
        );

        assertEquals(
                new Term.Str(SymbolTable.DEFAULT_SYMBOLS_OFFSET + 2),
                e.evaluate(new HashMap<>(),  new TemporarySymbolTable(symbols)).get()
        );
    }

    @Test
    public void testContainsStr() {
        SymbolTable symbols = new SymbolTable();
        symbols.add("ab");
        symbols.add("b");


        Expression e = new Expression(new ArrayList<Op>(Arrays.asList(
                new Op.Value(new Term.Str(SymbolTable.DEFAULT_SYMBOLS_OFFSET)),
                new Op.Value(new Term.Str(SymbolTable.DEFAULT_SYMBOLS_OFFSET + 1)),
                new Op.Binary(Op.BinaryOp.Contains)
        )));

        assertEquals(
                "\"ab\".contains(\"b\")",
                e.print(symbols).get()
        );

        assertEquals(
                new Term.Bool(true),
                e.evaluate(new HashMap<>(),  new TemporarySymbolTable(symbols)).get()
        );
    }

    @Test
    public void testNegativeContainsStr() {
        SymbolTable symbols = new SymbolTable();
        symbols.add("ab");
        symbols.add("b");


        Expression e = new Expression(new ArrayList<Op>(Arrays.asList(
                new Op.Value(new Term.Str(SymbolTable.DEFAULT_SYMBOLS_OFFSET)),
                new Op.Value(new Term.Str(SymbolTable.DEFAULT_SYMBOLS_OFFSET + 1)),
                new Op.Binary(Op.BinaryOp.Contains),
                new Op.Unary(Op.UnaryOp.Negate)
        )));

        assertEquals(
                "!\"ab\".contains(\"b\")",
                e.print(symbols).get()
        );

        assertEquals(
                new Term.Bool(false),
                e.evaluate(new HashMap<>(),  new TemporarySymbolTable(symbols)).get()
        );
    }
}
