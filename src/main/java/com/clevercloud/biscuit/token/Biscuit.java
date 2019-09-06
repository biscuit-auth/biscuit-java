package com.clevercloud.biscuit.token;

import cafe.cryptography.curve25519.RistrettoElement;
import com.clevercloud.biscuit.datalog.Fact;
import com.clevercloud.biscuit.datalog.Rule;
import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.error.LogicError;
import com.clevercloud.biscuit.token.format.SerializedBiscuit;
import com.clevercloud.biscuit.error.Error;

import io.vavr.control.Either;
import io.vavr.control.Option;
import static io.vavr.API.Left;
import static io.vavr.API.Right;

import java.security.KeyPair;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Biscuit {
    /*public final Block authority;
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

        Either<Error.FormatError, SerializedBiscuit> container = new SerializedBiscuit(rng, root, authority);
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

    public Either<Error.FormatError, SerializedBiscuit> from_bytes(byte[] data, RistrettoElement root)  {

    }

    public Either<Error.FormatError, SerializedBiscuit> from_sealed(byte[] data, byte[] secret)  {

    }

    public Either<Error.FormatError, byte[]> serialize() {}
    public Either<Error.FormatError, byte[]> seal(byte[] secret) {}

    public Either<LogicError, Void> check(List<Fact> ambient_facts, List<Rule> ambient_rules, List<Rule> ambien_caveats){}

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
