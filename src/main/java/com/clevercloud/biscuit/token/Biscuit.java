package com.clevercloud.biscuit.token;

import com.clevercloud.biscuit.crypto.KeyPair;
import com.clevercloud.biscuit.crypto.PublicKey;
import com.clevercloud.biscuit.datalog.*;
import com.clevercloud.biscuit.error.FailedCaveat;
import com.clevercloud.biscuit.error.LogicError;
import com.clevercloud.biscuit.token.format.SealedBiscuit;
import com.clevercloud.biscuit.token.format.SerializedBiscuit;
import com.clevercloud.biscuit.error.Error;

import io.vavr.control.Either;
import io.vavr.control.Option;
import static io.vavr.API.Left;
import static io.vavr.API.Right;

import java.security.SecureRandom;
import java.util.*;

/**
 * Biscuit auth token
 */
public class Biscuit {
    final Block authority;
    final List<Block> blocks;
    final SymbolTable symbols;
    final Option<SerializedBiscuit> container;


    /**
     * Creates a token builder
     *
     * this function uses the default symbol table
     *
     * @param rng random number generator
     * @param root root private key
     * @return
     */
    public static com.clevercloud.biscuit.token.builder.Biscuit builder(final SecureRandom rng, final KeyPair root) {
        return new com.clevercloud.biscuit.token.builder.Biscuit(rng, root, default_symbol_table());
    }

    /**
     * Creates a token builder
     *
     * @param rng random number generator
     * @param root root private key
     * @param symbols symbol table
     * @return
     */
    public static com.clevercloud.biscuit.token.builder.Biscuit builder(final SecureRandom rng, final KeyPair root, SymbolTable symbols) {
        return new com.clevercloud.biscuit.token.builder.Biscuit(rng, root, symbols);
    }

    /**
     * Creates a token
     * @param rng random number generator
     * @param root root private key
     * @param authority authority block
     * @return
     */
    static public Either<Error, Biscuit> make(final SecureRandom rng, final KeyPair root, final SymbolTable symbols, final Block authority) {
        if(!Collections.disjoint(symbols.symbols, authority.symbols.symbols)) {
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

    /**
     * Deserializes a Biscuit token from a byte array
     *
     * This checks the signature, but does not verify that the first key is the root key,
     * to allow appending blocks without knowing about the root key.
     *
     * The root key check is performed in the verify method
     *
     * This method uses the default symbol table
     * @param data
     * @return
     */
    static public Either<Error, Biscuit> from_bytes(byte[] data)  {
        return Biscuit.from_bytes_with_symbols(data, default_symbol_table());
    }

    /**
     * Deserializes a Biscuit token from a byte array
     *
     * This checks the signature, but does not verify that the first key is the root key,
     * to allow appending blocks without knowing about the root key.
     *
     * The root key check is performed in the verify method
     * @param data
     * @return
     */
    static public Either<Error, Biscuit> from_bytes_with_symbols(byte[] data, SymbolTable symbols)  {
        Either<Error, SerializedBiscuit> res = SerializedBiscuit.from_bytes(data);
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

    /**
     * Creates a verifier for this token
     *
     * This function checks that the root key is the one we expect
     * @param root root public key
     * @return
     */
    public Either<Error, Verifier> verify(PublicKey root) {
        return Verifier.make(this, Option.some(root));
    }

    public Either<Error, Verifier> verify_sealed() {
        return Verifier.make(this, Option.none());
    }

    /**
     * Serializes a token to a byte array
     * @return
     */
    public Either<Error.FormatError, byte[]> serialize() {
        if(this.container.isEmpty()) {
            return Left(new Error().new FormatError().new SerializationError("no internal container"));
        }
        return this.container.get().serialize();
    }


    public static Either<Error, Biscuit> from_sealed(byte[] data, byte[] secret)  {
        //FIXME: needs a version of from_sealed with custom symbol table support
        SymbolTable symbols = default_symbol_table();

        Either<Error, SealedBiscuit> res = SealedBiscuit.from_bytes(data, secret);
        if(res.isLeft()) {
            Error e = res.getLeft();
            return Left(e);
        }

        SealedBiscuit ser = res.get();
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

        for(String s: authority.symbols.symbols) {
            symbols.add(s);
        }

        for(Block b: blocks) {
            for(String s: b.symbols.symbols) {
                symbols.add(s);
            }
        }

        return Right(new Biscuit(authority, blocks, symbols, Option.none()));
    }

    public Either<Error.FormatError, byte[]> seal(byte[] secret) {
        Either<Error.FormatError, SealedBiscuit> res = SealedBiscuit.make(authority, blocks, secret);
        if(res.isLeft()) {
            Error.FormatError e = res.getLeft();
            return Left(e);
        }

        SealedBiscuit b = res.get();
        return b.serialize();
    }

    public boolean is_sealed() {
        return this.container.isEmpty();
    }

    /**
     * Verifies that a token is valid for a root public key
     * @param public_key
     * @return
     */
    public Either<Error, Void> check_root_key(PublicKey public_key) {
        if (this.container.isEmpty()) {
            return Left(new Error().new Sealed());
        } else {
            return this.container.get().check_root_key(public_key);
        }
    }

    Either<Error, Void> check(SymbolTable symbols, List<Fact> ambient_facts, List<Rule> ambient_rules,
                              List<Rule> verifier_authority_caveats, List<Rule> verifier_block_caveats){
        World world = new World();
        long authority_index = symbols.get("authority").get();
        long ambient_index = symbols.get("ambient").get();

        for(Fact fact: this.authority.facts) {
            if(!fact.predicate().ids().get(0).equals(new ID.Symbol(authority_index))) {
                return Left(new Error().new FailedLogic(new LogicError().new InvalidAuthorityFact(symbols.print_fact(fact))));
            }

            world.add_fact(fact);
        }

        for(Rule rule: this.authority.rules) {
            world.add_rule(rule);
        }

        // check that all generated facts have the authority ID
        for(Fact fact: world.facts()) {
            if(!fact.predicate().ids().get(0).equals(new ID.Symbol(authority_index))) {
                return Left(new Error().new FailedLogic(new LogicError().new InvalidAuthorityFact(symbols.print_fact(fact))));
            }

            world.add_fact(fact);
        }

        for(Fact fact: ambient_facts) {
            if(!fact.predicate().ids().get(0).equals(new ID.Symbol(ambient_index))) {
                return Left(new Error().new FailedLogic(new LogicError().new InvalidAmbientFact(symbols.print_fact(fact))));
            }

            world.add_fact(fact);
        }

        for(Rule rule: ambient_rules) {
            world.add_rule(rule);
        }

        //System.out.println("world after adding ambient rules:\n"+symbols.print_world(world));
        world.run();
        //System.out.println("world after running rules:\n"+symbols.print_world(world));

        // we only keep the verifier rules
        world.clearRules();
        for(Rule rule: ambient_rules) {
            world.add_rule(rule);
        }

        ArrayList<FailedCaveat> errors = new ArrayList<>();
        for (int j = 0; j < this.authority.caveats.size(); j++) {
            Set<Fact> res = world.query_rule(this.authority.caveats.get(j));
            if (res.isEmpty()) {
                errors.add(new FailedCaveat().
                        new FailedBlock(0, j, symbols.print_rule(this.authority.caveats.get(j))));
            }
        }

        for (int j = 0; j < verifier_authority_caveats.size(); j++) {
            Set<Fact> res = world.query_rule(verifier_authority_caveats.get(j));
            if (res.isEmpty()) {
                errors.add(new FailedCaveat().
                        new FailedVerifier(0, j, symbols.print_rule(verifier_authority_caveats.get(j))));
            }
        }

        for(int i = 0; i < this.blocks.size(); i++) {
            if(this.blocks.get(i).index != i+1) {
                return Left(new Error().new InvalidBlockIndex(1 + this.blocks.size(), this.blocks.get(i).index));
            }

            World w = new World(world);
            Either<LogicError, Void> res = this.blocks.get(i).check(i, w, symbols, verifier_block_caveats);
            if(res.isLeft()) {

                LogicError e = res.getLeft();
                Option<List<FailedCaveat>> optErr = e.failed_caveats();
                if(optErr.isEmpty()) {
                    return Left(new Error().new FailedLogic(e));
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
            return Left(new Error().new FailedLogic(new LogicError().new FailedCaveats(errors)));
        }
    }

    /**
     * Creates a Block builder
     * @return
     */
    public com.clevercloud.biscuit.token.builder.Block create_block() {
        return new com.clevercloud.biscuit.token.builder.Block(1+this.blocks.size(), new SymbolTable(this.symbols));
    }

    /**
     * Generates a new token from an existing one and a new block
     * @param rng random number generator
     * @param keypair ephemeral key pair
     * @param block new block (should be generated from a Block builder)
     * @return
     */
    public Either<Error, Biscuit> append(final SecureRandom rng, final KeyPair keypair, Block block) {
        if(this.container.isEmpty()) {
            return Left(new Error().new Sealed());
        }

        if(!Collections.disjoint(this.symbols.symbols, block.symbols.symbols)) {
            return Left(new Error().new SymbolTableOverlap());
        }

        if(block.index != 1 + this.blocks.size()) {
            return Left(new Error().new InvalidBlockIndex(1 + this.blocks.size(), block.index));
        }

        Either<Error.FormatError, SerializedBiscuit> containerRes = this.container.get().append(rng, keypair, block);
        if(containerRes.isLeft()) {
            Error.FormatError e = containerRes.getLeft();
            return Left(e);
        }
        SerializedBiscuit container = containerRes.get();

        SymbolTable symbols = new SymbolTable(this.symbols);
        for(String s: block.symbols.symbols) {
            symbols.add(s);
        }

        ArrayList<Block> blocks = new ArrayList<>();
        for(Block b: this.blocks) {
            blocks.add(b);
        }
        blocks.add(block);

        return Right(new Biscuit(this.authority, blocks, symbols, Option.some(container)));
    }

    public List<Option<String>> context() {
        ArrayList res = new ArrayList();
        if(this.authority.context.isEmpty()) {
            res.add(Option.none());
        } else {
            res.add(Option.some(this.authority.context));
        }

        for(Block b: this.blocks) {
            if(b.context.isEmpty()) {
                res.add(Option.none());
            } else {
                res.add(Option.some(b.context));
            }
        }

        return res;
    }

    /**
     * Prints a token's content
     */
    public String print() {
        StringBuilder s = new StringBuilder();
        s.append("Biscuit {\n\tsymbols: ");
        s.append(this.symbols.symbols);
        s.append("\n\tauthority: ");
        s.append(this.authority.print(this.symbols));
        s.append("\n\tblocks: [\n");
        for(Block b: this.blocks) {
            s.append("\t\t");
            s.append(b.print(this.symbols));
            s.append("\n");
        }
        s.append("\t]\n}");

        return s.toString();
    }

    /**
     * Default symbols list
     */
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
    }
}
