package com.clevercloud.biscuit.token.builder;


import com.clevercloud.biscuit.datalog.Fact;
import com.clevercloud.biscuit.datalog.Rule;
import com.clevercloud.biscuit.datalog.Caveat;
import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.datalog.constraints.Constraint;
import com.clevercloud.biscuit.datalog.constraints.ConstraintKind;
import com.clevercloud.biscuit.datalog.constraints.DateConstraint;
import com.clevercloud.biscuit.datalog.constraints.StrConstraint;
import static com.clevercloud.biscuit.token.builder.Utils.*;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Block {
    long index;
    int symbol_start;
    SymbolTable symbols;
    String context;
    List<Fact> facts;
    List<Rule> rules;
    List<Caveat> caveats;

    public Block(long index, SymbolTable base_symbols) {
        this.index = index;
        this.symbol_start = base_symbols.symbols.size();
        this.symbols = new SymbolTable(base_symbols);
        this.context = "";
        this.facts = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.caveats = new ArrayList<>();
    }

    public void add_fact(com.clevercloud.biscuit.token.builder.Fact f) {
        this.facts.add(f.convert(this.symbols));
    }

    public void add_rule(com.clevercloud.biscuit.token.builder.Rule rule) {
        this.rules.add(rule.convert(this.symbols));
    }

    public void add_caveat(com.clevercloud.biscuit.token.builder.Caveat caveat) {
        this.caveats.add(caveat.convert(this.symbols));
    }

    public  void set_context(String context) {
        this.context = context;
    }

    public com.clevercloud.biscuit.token.Block build() {
        SymbolTable symbols = new SymbolTable();

        for(int i = this.symbol_start; i < this.symbols.symbols.size(); i++) {
            symbols.add(this.symbols.symbols.get(i));
        }

        return new com.clevercloud.biscuit.token.Block(this.index, symbols, this.context, this.facts, this.rules, this.caveats);
    }

    public void check_right(String right) {
        ArrayList<com.clevercloud.biscuit.token.builder.Rule> queries = new ArrayList<>();
        queries.add(rule(
                "check_right",
                Arrays.asList(s(right)),
                Arrays.asList(
                        pred("resource", Arrays.asList(s("ambient"), var("resource"))),
                        pred("operation", Arrays.asList(s("ambient"), s(right))),
                        pred("right", Arrays.asList(s("authority"), var("resource"), s(right)))
                )
        ));
        this.add_caveat(new com.clevercloud.biscuit.token.builder.Caveat(queries));
    }

    public void resource_prefix(String prefix) {
        ArrayList<com.clevercloud.biscuit.token.builder.Rule> queries = new ArrayList<>();

        queries.add(constrained_rule(
                "prefix",
                Arrays.asList(var("resource")),
                Arrays.asList(pred("resource", Arrays.asList(s("ambient"), var("resource")))),
                Arrays.asList(new Expression.Binary(Expression.Op.Prefix, new Expression.Value(var("resource")),
                        new Expression.Value(string(prefix))))
        ));
        this.add_caveat(new com.clevercloud.biscuit.token.builder.Caveat(queries));
    }

    public void resource_suffix(String suffix) {
        ArrayList<com.clevercloud.biscuit.token.builder.Rule> queries = new ArrayList<>();

        queries.add(constrained_rule(
                "suffix",
                Arrays.asList(var("resource")),
                Arrays.asList(pred("resource", Arrays.asList(s("ambient"), var("resource")))),
                Arrays.asList(new Expression.Binary(Expression.Op.Suffix, new Expression.Value(var("resource")),
                        new Expression.Value(string(suffix))))
        ));
        this.add_caveat(new com.clevercloud.biscuit.token.builder.Caveat(queries));
    }

    public void expiration_date(Date d) {
        ArrayList<com.clevercloud.biscuit.token.builder.Rule> queries = new ArrayList<>();

        queries.add(constrained_rule(
                "expiration",
                Arrays.asList(var("date")),
                Arrays.asList(pred("time", Arrays.asList(s("ambient"), var("date")))),
                Arrays.asList(new Expression.Binary(Expression.Op.LessOrEqual, new Expression.Value(var("date")),
                        new Expression.Value(date(d))))
        ));
        this.add_caveat(new com.clevercloud.biscuit.token.builder.Caveat(queries));
    }
}
