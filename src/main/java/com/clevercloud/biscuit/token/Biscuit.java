package com.clevercloud.biscuit.token;

import cafe.cryptography.curve25519.RistrettoElement;
import com.clevercloud.biscuit.crypto.KeyPair;
import com.clevercloud.biscuit.datalog.*;
import com.clevercloud.biscuit.error.FailedCaveat;
import com.clevercloud.biscuit.error.LogicError;
import com.clevercloud.biscuit.token.format.SerializedBiscuit;
import com.clevercloud.biscuit.error.Error;

import io.vavr.control.Either;
import io.vavr.control.Option;
import static io.vavr.API.Left;
import static io.vavr.API.Right;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Biscuit {
    public final Block authority;
    public final List<Block> blocks;
    public final SymbolTable symbols;
    public final Option<SerializedBiscuit> container;

    public Either<Error, Biscuit> make(final SecureRandom rng, final KeyPair root, final Block authority) {
        SymbolTable symbols = default_symbol_table();

        if(Collections.disjoint(symbols.symbols, authority.symbols.symbols)) {
            return Left(new Error().new SymbolTableOverlap());
        }

        if(authority.index != 0) {
            return Left(new Error().new InvalidAuthorityIndex(authority.index));
        }

        symbols.symbols.addAll(authority.symbols.symbols);
        ArrayList<Block> blocks = new ArrayList<>();

        Either<Error.FormatError, SerializedBiscuit> container = SerializedBiscuit.make(rng, root, authority);
        if(container.isLeft()) {
            Error.FormatError e = container.getLeft();
            return Left(e);
        } else {
            Option<SerializedBiscuit> c = Option.some(container.get());
            return Right(new Biscuit(authority, blocks, symbols, c));
        }
    }

    Biscuit(Block authority, List<Block> blocks, SymbolTable symbols, Option<SerializedBiscuit> container) {
        this.authority = authority;
        this.blocks = blocks;
        this.symbols = symbols;
        this.container = container;
    }

    public Either<Error, Biscuit> from_bytes(byte[] data, RistrettoElement root)  {
        Either<Error, SerializedBiscuit> res = SerializedBiscuit.from_bytes(data, root);
        if(res.isLeft()) {
            Error e = res.getLeft();
            return Left(e);
        }

        SerializedBiscuit ser = res.get();
        Either<Error.FormatError, Block> authRes = Block.from_bytes(ser.authority);
        if(authRes.isLeft()){
            Error e = authRes.getLeft();
            return Left(e);
        }
        Block authority = authRes.get();

        ArrayList<Block> blocks = new ArrayList<>();
        for(byte[] bdata: ser.blocks) {
            Either<Error.FormatError, Block> blockRes = Block.from_bytes(bdata);
            if(blockRes.isLeft()) {
                Error e = blockRes.getLeft();
                return Left(e);
            }
            blocks.add(blockRes.get());
        }

        SymbolTable symbols = default_symbol_table();
        for(String s: authority.symbols.symbols) {
            symbols.add(s);
        }

        for(Block b: blocks) {
            for(String s: b.symbols.symbols) {
                symbols.add(s);
            }
        }

        return Right(new Biscuit(authority, blocks, symbols, Option.some(ser)));
    }


    public Either<Error.FormatError, byte[]> serialize() {
        if(this.container.isEmpty()) {
            return Left(new Error().new FormatError().new SerializationError("no internal container"));
        }
        return this.container.get().serialize();
    }


    /*public Either<Error.FormatError, SerializedBiscuit> from_sealed(byte[] data, byte[] secret)  {

    }*/
    //public Either<Error.FormatError, byte[]> seal(byte[] secret) {}

    public Either<LogicError, Void> check(List<Fact> ambient_facts, List<Rule> ambient_rules, List<Rule> ambient_caveats){
        World world = new World();
        long authority_index = this.symbols.get("authority").get();
        long ambient_index = this.symbols.get("ambient").get();

        for(Fact fact: this.authority.facts) {
            if(fact.predicate().ids().get(0) != new ID.Symbol(authority_index)) {
                return Left(new LogicError().new InvalidAuthorityFact(this.symbols.print_fact(fact)));
            }

            world.add_fact(fact);
        }

        for(Rule rule: this.authority.caveats) {
            world.add_rule(rule);
        }

        // check that all generated facts have the authority ID
        for(Fact fact: world.facts()) {
            if(fact.predicate().ids().get(0) != new ID.Symbol(authority_index)) {
                return Left(new LogicError().new InvalidAuthorityFact(this.symbols.print_fact(fact)));
            }

            world.add_fact(fact);
        }

        for(Fact fact: ambient_facts) {
            if(fact.predicate().ids().get(0) != new ID.Symbol(ambient_index)) {
                return Left(new LogicError().new InvalidAmbientFact(this.symbols.print_fact(fact)));
            }

            world.add_fact(fact);
        }

        for(Rule rule: ambient_rules) {
            world.add_rule(rule);
        }

        world.run();

        // we only keep the verifier rules
        world.clearRules();
        for(Rule rule: ambient_rules) {
            world.add_rule(rule);
        }

        ArrayList<FailedCaveat> errors = new ArrayList<>();
        for(int i = 0; i < this.blocks.size(); i++) {
            World w = new World(world);
            Either<LogicError, Void> res = this.blocks.get(i).check(i, w, this.symbols, ambient_caveats);
            if(res.isLeft()) {

                LogicError e = res.getLeft();
                Option<List<FailedCaveat>> optErr = e.failed_caveats();
                if(optErr.isEmpty()) {
                    return Left(e);
                } else {
                    for(FailedCaveat f: optErr.get()) {
                        errors.add(f);
                    }
                }
            }
        }

        if(errors.isEmpty()) {
            return Right(null);
        } else {
            return Left(new LogicError().new FailedCaveats(errors));
        }
    }

    public BlockBuilder create_block() {}

    public Either<Error, SerializedBiscuit> append(final SecureRandom rng, final KeyPair keypair, Block block) {}

    public void adjust_authority_symbols(Block block){}
    public void adjust_block_symbols(Block block){}
    public String print() {}

    static public SymbolTable default_symbol_table() {
        SymbolTable syms = new SymbolTable();
        syms.insert("authority");
        syms.insert("ambient");
        syms.insert("resource");
        syms.insert("operation");
        syms.insert("right");
        syms.insert("current_time");
        syms.insert("revocation_id");

        return syms;
    }*/
}
