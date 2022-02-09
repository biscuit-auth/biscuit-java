package com.clevercloud.biscuit.token;

import biscuit.format.schema.Schema;
import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.datalog.*;
import com.clevercloud.biscuit.error.FailedCheck;
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
 * Represents a token's block with its checks
 */
public class Block {
    final SymbolTable symbols;
    final String context;
    final List<Fact> facts;
    final List<Rule> rules;
    final List<Check> checks;
    final long version;

    /**
     * creates a new block
     *
     * @param index
     * @param base_symbols
     */
    public Block(SymbolTable base_symbols) {
        this.symbols = base_symbols;
        this.context = "";
        this.facts = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.checks = new ArrayList<>();
        this.version = SerializedBiscuit.MAX_SCHEMA_VERSION;
    }

    /**
     * creates a new block
     *
     * @param index
     * @param base_symbols
     * @param facts
     * @param checks
     */
    public Block(SymbolTable base_symbols, String context, List<Fact> facts, List<Rule> rules, List<Check> checks) {
        this.symbols = base_symbols;
        this.context = context;
        this.facts = facts;
        this.rules = rules;
        this.checks = checks;
        this.version = SerializedBiscuit.MAX_SCHEMA_VERSION;
    }

    /*
    Either<LogicError, Void> check(long i, World w, SymbolTable symbols, List<Check> authorizer_checks,
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

        ArrayList<FailedCheck> errors = new ArrayList<>();

        for (int j = 0; j < this.checks.size(); j++) {
            boolean successful = false;
            Check c = this.checks.get(j);

            for(int k = 0; k < c.queries().size(); k++) {
                Set<Fact> res = world.query_rule(c.queries().get(k));
                if (!res.isEmpty()) {
                    successful = true;
                    break;
                }
            }

            if (!successful) {
                errors.add(new FailedCheck.FailedBlock(i, j, symbols.print_check(this.checks.get(j))));
            }
        }

        for (int j = 0; j < authorizer_checks.size(); j++) {
            boolean successful = false;
            Check c = authorizer_checks.get(j);

            for(int k = 0; k < c.queries().size(); k++) {
                Set<Fact> res = world.query_rule(c.queries().get(k));
                if (!res.isEmpty()) {
                    successful = true;
                    break;
                }
            }

            if (!successful) {
                errors.add(new FailedCheck.FailedAuthorizer(j, symbols.print_check(authorizer_checks.get(j))));
            }
        }

        for(String name: queries.keySet()) {
            Set<Fact> res = world.query_rule(queries.get(name));
            query_results.get(name).put(new Long(this.index), res);
        }

        if (errors.isEmpty()) {
            return Right(null);
        } else {
            return Left(new LogicError.FailedChecks(errors));
        }
    }*/

    /**
     * pretty printing for a block
     *
     * @param symbol_table
     * @return
     */
    public String print(SymbolTable symbol_table) {
        StringBuilder s = new StringBuilder();

        s.append("Block");
        s.append(" {\n\t\tsymbols: ");
        s.append(this.symbols.symbols);
        s.append("\n\t\tcontext: ");
        s.append(this.context);
        s.append("\n\t\tfacts: [");
        for (Fact f : this.facts) {
            s.append("\n\t\t\t");
            s.append(symbol_table.print_fact(f));
        }
        s.append("\n\t\t]\n\t\trules: [");
        for (Rule r : this.rules) {
            s.append("\n\t\t\t");
            s.append(symbol_table.print_rule(r));
        }
        s.append("\n\t\t]\n\t\tchecks: [");
        for (Check c : this.checks) {
            s.append("\n\t\t\t");
            s.append(symbol_table.print_check(c));
        }
        s.append("\n\t\t]\n\t}");

        return s.toString();
    }

    /**
     * Serializes a Block to its Protobuf representation
     *
     * @return
     */
    public Schema.Block serialize() {
        Schema.Block.Builder b = Schema.Block.newBuilder();

        for (int i = 0; i < this.symbols.symbols.size(); i++) {
            b.addSymbols(this.symbols.symbols.get(i));
        }

        if (!this.context.isEmpty()) {
            b.setContext(this.context);
        }

        for (int i = 0; i < this.facts.size(); i++) {
            b.addFactsV2(this.facts.get(i).serialize());
        }

        for (int i = 0; i < this.rules.size(); i++) {
            b.addRulesV2(this.rules.get(i).serialize());
        }

        for (int i = 0; i < this.checks.size(); i++) {
            b.addChecksV2(this.checks.get(i).serialize());
        }

        b.setVersion(SerializedBiscuit.MAX_SCHEMA_VERSION);
        return b.build();
    }

    /**
     * Deserializes a block from its Protobuf representation
     *
     * @param b
     * @return
     */
    static public Either<Error.FormatError, Block> deserialize(Schema.Block b) {
        int version = b.getVersion();
        if (version > SerializedBiscuit.MAX_SCHEMA_VERSION) {
            return Left(new Error.FormatError.Version(SerializedBiscuit.MAX_SCHEMA_VERSION, version));
        }

        SymbolTable symbols = new SymbolTable();
        for (String s : b.getSymbolsList()) {
            symbols.add(s);
        }

        ArrayList<Fact> facts = new ArrayList<>();
        ArrayList<Rule> rules = new ArrayList<>();
        ArrayList<Check> checks = new ArrayList<>();

        if (version == 2) {
            for (Schema.FactV2 fact : b.getFactsV2List()) {
                Either<Error.FormatError, Fact> res = Fact.deserializeV2(fact);
                if (res.isLeft()) {
                    Error.FormatError e = res.getLeft();
                    return Left(e);
                } else {
                    facts.add(res.get());
                }
            }


            for (Schema.RuleV2 rule : b.getRulesV2List()) {
                Either<Error.FormatError, Rule> res = Rule.deserializeV2(rule);
                if (res.isLeft()) {
                    Error.FormatError e = res.getLeft();
                    return Left(e);
                } else {
                    rules.add(res.get());
                }
            }


            for (Schema.CheckV2 check : b.getChecksV2List()) {
                Either<Error.FormatError, Check> res = Check.deserializeV2(check);
                if (res.isLeft()) {
                    Error.FormatError e = res.getLeft();
                    return Left(e);
                } else {
                    checks.add(res.get());
                }
            }
            return Right(new Block(symbols, b.getContext(), facts, rules, checks));
        } else {
            return Left(new Error.FormatError.Version(SerializedBiscuit.MAX_SCHEMA_VERSION, version));
        }
    }

    /**
     * Deserializes a Block from a byte array
     *
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
        } catch (IOException e) {
            return Left(new Error.FormatError.SerializationError(e.toString()));
        }
    }
}

