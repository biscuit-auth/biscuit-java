package com.clevercloud.biscuit.datalog.expressions;

import com.clevercloud.biscuit.datalog.ID;
import com.clevercloud.biscuit.datalog.SymbolTable;
import io.vavr.control.Option;

import java.util.ArrayList;
import java.util.HashMap;

public class Expression {
    private final ArrayList<Op> ops;

    public Expression(ArrayList<Op> ops) {
        this.ops = ops;
    }

    public Option<ID> evaluate(HashMap<Long, ID> variables) {
        throw new UnsupportedOperationException("not implemented");
    }

    public Option<String> print(SymbolTable symbols) {
        throw new UnsupportedOperationException("not implemented");
    }
}
