package com.clevercloud.biscuit.token;

import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.datalog.*;
import com.clevercloud.biscuit.error.FailedCaveat;
import com.clevercloud.biscuit.error.LogicError;
import io.vavr.control.Either;

import java.util.ArrayList;
import java.util.Set;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

public class Block {
    final long index;
    final SymbolTable symbols;
    final ArrayList<Fact> facts;
    final ArrayList<Rule> caveats;

    public Block(long index, SymbolTable base_symbols) {
        this.index = index;
        this.symbols = base_symbols;
        this.facts = new ArrayList<>();
        this.caveats = new ArrayList<>();
    }

    public ID symbol_add(final String symbol) {
        return this.symbols.add(symbol);
    }

    public long insert(final String symbol) {
        return this.symbols.insert(symbol);
    }

    public Either<LogicError, Void> check(long i, World w, SymbolTable symbols, ArrayList<Rule> verifier_caveats) {
        World world = new World(w);
        long authority_index = this.symbols.get("authority").get().longValue();
        long ambient_index = this.symbols.get("ambient").get().longValue();

        for(Fact fact: this.facts) {
            if(fact.predicate().ids().get(0) == new ID.Symbol(authority_index) ||
              fact.predicate().ids().get(0) == new ID.Symbol(ambient_index)) {
                return Left(new LogicError().new InvalidBlockFact(i, symbols.print_fact(fact)));
            }

            world.add_fact(fact);
        }

        world.run();

        ArrayList<FailedCaveat> errors = new ArrayList<>();

        for(int j = 0; j < this.caveats.size(); j++) {
            Set<Fact> res = world.query_rule((this.caveats.get(j)));
            if (res.isEmpty()) {
                errors.add(new FailedCaveat().
                        new FailedBlock(i, j, symbols.print_rule(this.caveats.get(j))));
            }
        }

        for(int j = 0; j < verifier_caveats.size(); j++) {
            Set<Fact> res = world.query_rule((verifier_caveats.get(j)));
            if (res.isEmpty()) {
                errors.add(new FailedCaveat().
                        new FailedVerifier(j, symbols.print_rule(verifier_caveats.get(j))));
            }
        }

        if(errors.isEmpty()) {
            return Right(null);
        } else {
            return Left(new LogicError().new FailedCaveats(errors));
        }
    }
}

