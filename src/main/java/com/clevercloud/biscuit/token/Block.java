package com.clevercloud.biscuit.token;

import biscuit.format.schema.Schema;
import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.datalog.*;
import com.clevercloud.biscuit.error.FailedCaveat;
import com.clevercloud.biscuit.error.LogicError;
import com.google.protobuf.InvalidProtocolBufferException;
import io.vavr.control.Either;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

public class Block {
    final long index;
    final SymbolTable symbols;
    final List<Fact> facts;
    final List<Rule> caveats;

    public Block(long index, SymbolTable base_symbols) {
        this.index = index;
        this.symbols = base_symbols;
        this.facts = new ArrayList<>();
        this.caveats = new ArrayList<>();
    }

    public Block(long index, SymbolTable base_symbols, List<Fact> facts, List<Rule> caveats) {
        this.index = index;
        this.symbols = base_symbols;
        this.facts = facts;
        this.caveats = caveats;
    }

    public ID symbol_add(final String symbol) {
        return this.symbols.add(symbol);
    }

    public long symbol_insert(final String symbol) {
        return this.symbols.insert(symbol);
    }

    public Either<LogicError, Void> check(long i, World w, SymbolTable symbols, List<Rule> verifier_caveats) {
        World world = new World(w);
        long authority_index = this.symbols.get("authority").get().longValue();
        long ambient_index = this.symbols.get("ambient").get().longValue();

        for (Fact fact : this.facts) {
            if (fact.predicate().ids().get(0) == new ID.Symbol(authority_index) ||
                    fact.predicate().ids().get(0) == new ID.Symbol(ambient_index)) {
                return Left(new LogicError().new InvalidBlockFact(i, symbols.print_fact(fact)));
            }

            world.add_fact(fact);
        }

        world.run();

        ArrayList<FailedCaveat> errors = new ArrayList<>();

        for (int j = 0; j < this.caveats.size(); j++) {
            Set<Fact> res = world.query_rule((this.caveats.get(j)));
            if (res.isEmpty()) {
                errors.add(new FailedCaveat().
                        new FailedBlock(i, j, symbols.print_rule(this.caveats.get(j))));
            }
        }

        for (int j = 0; j < verifier_caveats.size(); j++) {
            Set<Fact> res = world.query_rule((verifier_caveats.get(j)));
            if (res.isEmpty()) {
                errors.add(new FailedCaveat().
                        new FailedVerifier(j, symbols.print_rule(verifier_caveats.get(j))));
            }
        }

        if (errors.isEmpty()) {
            return Right(null);
        } else {
            return Left(new LogicError().new FailedCaveats(errors));
        }
    }

    public String print(SymbolTable symbol_table) {
        StringBuilder s = new StringBuilder();

        s.append("Block[");
        s.append(this.index);
        s.append("] {\n\t\tsymbols: ");
        s.append(this.symbols.symbols);
        s.append("\n\t\tfacts: [");
        for(Fact f: this.facts) {
            s.append("\n\t\t\t");
            s.append(symbol_table.print_fact(f));
        }
        s.append("\n\t\t]\n\t\trules: [");
        for(Rule r: this.caveats) {
            s.append("\n\t\t\t");
            s.append(symbol_table.print_rule(r));
        }
        s.append("\n\t\t]\n\t}");

        return s.toString();
    }

    public Schema.Block serialize() {
        Schema.Block.Builder b = Schema.Block.newBuilder()
                .setIndex((int) this.index);

        for (int i = 0; i < this.symbols.symbols.size(); i++) {
            b.addSymbols(this.symbols.symbols.get(i));
        }

        for (int i = 0; i < this.facts.size(); i++) {
            b.addFacts(this.facts.get(i).serialize());
        }

        for (int i = 0; i < this.caveats.size(); i++) {
            b.addCaveats(this.caveats.get(i).serialize());
        }

        return b.build();
    }

    static public Either<Error.FormatError, Block> deserialize(Schema.Block b) {
        SymbolTable symbols = new SymbolTable();
        for (String s : b.getSymbolsList()) {
            symbols.add(s);
        }

        ArrayList<Fact> facts = new ArrayList<>();
        for (Schema.Fact fact : b.getFactsList()) {
            Either<Error.FormatError, Fact> res = Fact.deserialize(fact);
            if (res.isLeft()) {
                Error.FormatError e = res.getLeft();
                return Left(e);
            } else {
                facts.add(res.get());
            }
        }

        ArrayList<Rule> caveats = new ArrayList<>();
        for (Schema.Rule caveat : b.getCaveatsList()) {
            Either<Error.FormatError, Rule> res = Rule.deserialize(caveat);
            if (res.isLeft()) {
                Error.FormatError e = res.getLeft();
                return Left(e);
            } else {
                caveats.add(res.get());
            }
        }

        return Right(new Block(b.getIndex(), symbols, facts, caveats));
    }

    static public Either<Error.FormatError, Block> from_bytes(byte[] slice) {
        try {
            Schema.Block data = Schema.Block.parseFrom(slice);
            return Block.deserialize(data);
        } catch (InvalidProtocolBufferException e) {
            return Left(new Error().new FormatError().new DeserializationError(e.toString()));
        }
    }

    public Either<Error.FormatError, byte[]> to_bytes() {
        Schema.Block b = this.serialize();
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            b.writeTo(stream);
            byte[] data = stream.toByteArray();
            return Right(data);
        } catch(IOException e) {
            return Left(new Error().new FormatError().new SerializationError(e.toString()));
        }
    }
}

