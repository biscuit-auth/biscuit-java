package com.clevercloud.biscuit.token;

import biscuit.format.schema.Schema;
import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.datalog.*;
import com.clevercloud.biscuit.error.FailedCaveat;
import com.clevercloud.biscuit.error.LogicError;
import com.clevercloud.biscuit.token.format.SerializedBiscuit;
import com.google.protobuf.InvalidProtocolBufferException;
import io.vavr.control.Either;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

/**
 * Represents a token's block with its caveats
 */
public class Block {
    final long index;
    final SymbolTable symbols;
    final String context;
    final List<Fact> facts;
    final List<Rule> rules;
    final List<Caveat> caveats;
    final long version;

    /**
     * creates a new block
     * @param index
     * @param base_symbols
     */
    public Block(long index, SymbolTable base_symbols) {
        this.index = index;
        this.symbols = base_symbols;
        this.context = "";
        this.facts = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.caveats = new ArrayList<>();
        this.version = SerializedBiscuit.MAX_SCHEMA_VERSION;
    }

    /**
     * creates a new block
     * @param index
     * @param base_symbols
     * @param facts
     * @param caveats
     */
    public Block(long index, SymbolTable base_symbols, String context, List<Fact> facts, List<Rule> rules, List<Caveat> caveats) {
        this.index = index;
        this.symbols = base_symbols;
        this.context=  context;
        this.facts = facts;
        this.rules = rules;
        this.caveats = caveats;
        this.version = SerializedBiscuit.MAX_SCHEMA_VERSION;
    }

    Either<LogicError, Void> check(long i, World w, SymbolTable symbols, List<Caveat> verifier_caveats,
                                   HashMap<String, Rule> queries, HashMap<String, HashMap<Long, Set<Fact>>> query_results) {
        World world = new World(w);
        long authority_index = symbols.get("authority").get().longValue();
        long ambient_index = symbols.get("ambient").get().longValue();

        for (Fact fact : this.facts) {
            if (fact.predicate().ids().get(0).equals(new ID.Symbol(authority_index)) ||
                    fact.predicate().ids().get(0).equals(new ID.Symbol(ambient_index))) {
                return Left(new LogicError.InvalidBlockFact(i, symbols.print_fact(fact)));
            }

            world.add_fact(fact);
        }

        for(Rule rule: this.rules) {
            world.add_rule(rule);
        }

        world.run();

        ArrayList<FailedCaveat> errors = new ArrayList<>();

        for (int j = 0; j < this.caveats.size(); j++) {
            boolean successful = false;
            Caveat c = this.caveats.get(j);

            for(int k = 0; k < c.queries().size(); k++) {
                Set<Fact> res = world.query_rule(c.queries().get(k));
                if (!res.isEmpty()) {
                    successful = true;
                    break;
                }
            }

            if (!successful) {
                errors.add(new FailedCaveat.FailedBlock(i, j, symbols.print_caveat(this.caveats.get(j))));
            }
        }

        for (int j = 0; j < verifier_caveats.size(); j++) {
            boolean successful = false;
            Caveat c = verifier_caveats.get(j);

            for(int k = 0; k < c.queries().size(); k++) {
                Set<Fact> res = world.query_rule(c.queries().get(k));
                if (!res.isEmpty()) {
                    successful = true;
                    break;
                }
            }

            if (!successful) {
                errors.add(new FailedCaveat.FailedVerifier(j, symbols.print_caveat(verifier_caveats.get(j))));
            }
        }

        for(String name: queries.keySet()) {
            Set<Fact> res = world.query_rule(queries.get(name));
            query_results.get(name).put(new Long(this.index), res);
        }

        if (errors.isEmpty()) {
            return Right(null);
        } else {
            return Left(new LogicError.FailedCaveats(errors));
        }
    }

    /**
     * pretty printing for a block
     * @param symbol_table
     * @return
     */
    public String print(SymbolTable symbol_table) {
        StringBuilder s = new StringBuilder();

        s.append("Block[");
        s.append(this.index);
        s.append("] {\n\t\tsymbols: ");
        s.append(this.symbols.symbols);
        s.append("\n\t\tcontext: ");
        s.append(this.context);
        s.append("\n\t\tfacts: [");
        for(Fact f: this.facts) {
            s.append("\n\t\t\t");
            s.append(symbol_table.print_fact(f));
        }
        s.append("\n\t\t]\n\t\trules: [");
        for(Rule r: this.rules) {
            s.append("\n\t\t\t");
            s.append(symbol_table.print_rule(r));
        }
        s.append("\n\t\t]\n\t\tcaveats: [");
        for(Caveat c: this.caveats) {
            s.append("\n\t\t\t");
            s.append(symbol_table.print_caveat(c));
        }
        s.append("\n\t\t]\n\t}");

        return s.toString();
    }

    /**
     * Serializes a Block to its Protobuf representation
     * @return
     */
    public Schema.Block serialize() {
        Schema.Block.Builder b = Schema.Block.newBuilder()
                .setIndex((int) this.index);

        for (int i = 0; i < this.symbols.symbols.size(); i++) {
            b.addSymbols(this.symbols.symbols.get(i));
        }

        if(!this.context.isEmpty()) {
            b.setContext(this.context);
        }

        for (int i = 0; i < this.facts.size(); i++) {
            b.addFacts(this.facts.get(i).serialize());
        }

        for (int i = 0; i < this.rules.size(); i++) {
            b.addRules(this.rules.get(i).serialize());
        }

        for (int i = 0; i < this.caveats.size(); i++) {
            b.addCaveats(this.caveats.get(i).serialize());
        }

        b.setVersion(SerializedBiscuit.MAX_SCHEMA_VERSION);
        return b.build();
    }

    /**
     * Deserializes a block from its Protobuf representation
     * @param b
     * @return
     */
    static public Either<Error.FormatError, Block> deserialize(Schema.Block b) {
        int version = b.getVersion();
        if(version > SerializedBiscuit.MAX_SCHEMA_VERSION) {
            return Left(new Error.FormatError.Version(SerializedBiscuit.MAX_SCHEMA_VERSION, version));
        }

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

        ArrayList<Rule> rules = new ArrayList<>();
        for (Schema.Rule rule : b.getRulesList()) {
            Either<Error.FormatError, Rule> res = Rule.deserialize(rule);
            if (res.isLeft()) {
                Error.FormatError e = res.getLeft();
                return Left(e);
            } else {
                rules.add(res.get());
            }
        }

        ArrayList<Caveat> caveats = new ArrayList<>();
        for (Schema.Caveat caveat : b.getCaveatsList()) {
            Either<Error.FormatError, Caveat> res = Caveat.deserialize(caveat);
            if (res.isLeft()) {
                Error.FormatError e = res.getLeft();
                return Left(e);
            } else {
                caveats.add(res.get());
            }
        }

        return Right(new Block(b.getIndex(), symbols, b.getContext(), facts, rules, caveats));
    }

    /**
     * Deserializes a Block from a byte array
     * @param slice
     * @return
     */
    static public Either<Error.FormatError, Block> from_bytes(byte[] slice) {
        try {
            Schema.Block data = Schema.Block.parseFrom(slice);
            return Block.deserialize(data);
        } catch (InvalidProtocolBufferException e) {
            return Left(new Error.FormatError.DeserializationError(e.toString()));
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
            return Left(new Error.FormatError.SerializationError(e.toString()));
        }
    }
}

