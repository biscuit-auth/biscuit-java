package com.clevercloud.biscuit.token;

import com.clevercloud.biscuit.crypto.KeyPair;
import com.clevercloud.biscuit.crypto.PublicKey;
import com.clevercloud.biscuit.datalog.*;
import com.clevercloud.biscuit.error.FailedCheck;
import com.clevercloud.biscuit.error.LogicError;
import com.clevercloud.biscuit.token.format.SealedBiscuit;
import com.clevercloud.biscuit.token.format.SerializedBiscuit;
import com.clevercloud.biscuit.error.Error;

import io.vavr.control.Either;
import io.vavr.control.Option;
import static io.vavr.API.Left;
import static io.vavr.API.Right;

import java.security.MessageDigest;
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
            return Left(new Error.SymbolTableOverlap());
        }

        if(authority.index != 0) {
            return Left(new Error.InvalidAuthorityIndex(authority.index));
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
     * Deserializes a Biscuit token from a hex string
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
    static public Either<Error, Biscuit> from_b64(String data)  {
        return Biscuit.from_bytes(Base64.getUrlDecoder().decode(data));
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
    public Either<Error, byte[]> serialize() {
        if(this.container.isEmpty()) {
            return Left(new Error.FormatError.SerializationError("no internal container"));
        }
        return this.container.get().serialize();
    }

    /**
     * Serializes a token to a base 64 String
     * @return
     */
    public Either<Error, String> serialize_b64() {
        return serialize().map(Base64.getUrlEncoder()::encodeToString);
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
            return Left(new Error.Sealed());
        } else {
            return this.container.get().check_root_key(public_key);
        }
    }

    Either<Error, World> generate_world() {
        World world = new World();
        long authority_index = symbols.get("authority").get();
        long ambient_index = symbols.get("ambient").get();

        for(Fact fact: this.authority.facts) {
            world.add_fact(fact);
        }

        for(Rule rule: this.authority.rules) {
            world.add_rule(rule);
        }

        for(int i = 0; i < this.blocks.size(); i++) {
            Block b = this.blocks.get(i);
            if (b.index != i + 1) {
                return Left(new Error.InvalidBlockIndex(1 + this.blocks.size(), this.blocks.get(i).index));
            }

            for (Fact fact : b.facts) {
                if (fact.predicate().ids().get(0).equals(new ID.Symbol(authority_index)) ||
                        fact.predicate().ids().get(0).equals(new ID.Symbol(ambient_index))) {
                    return Left(new Error.FailedLogic(new LogicError.InvalidBlockFact(i, symbols.print_fact(fact))));
                }

                world.add_fact(fact);
            }

            for (Rule rule : b.rules) {
                world.add_rule(rule);
            }
        }

        List<byte[]> revocation_ids = this.revocation_identifiers();
        long rev = symbols.get("revocation_id").get();
        for(int i = 0; i < revocation_ids.size(); i++) {
            byte[] id = revocation_ids.get(i);
            world.add_fact(new Fact(new Predicate(rev, Arrays.asList(new ID.Integer(i), new ID.Bytes(id)))));
        }

        return Right(world);
    }

    Either<Error, HashMap<String, Set<Fact>>> check(SymbolTable symbols, List<Fact> ambient_facts, List<Rule> ambient_rules,
                                                    List<Check> verifier_checks, HashMap<String, Rule> queries){
        Either<Error, World> wres = this.generate_world();

        if (wres.isLeft()) {
            Error e = wres.getLeft();
            return Left(e);
        }

        World world = wres.get();

        for(Fact fact: ambient_facts) {
            world.add_fact(fact);
        }

        for(Rule rule: ambient_rules) {
            world.add_rule(rule);
        }

        //System.out.println("world after adding ambient rules:\n"+symbols.print_world(world));
        world.run();
        //System.out.println("world after running rules:\n"+symbols.print_world(world));

        ArrayList<FailedCheck> errors = new ArrayList<>();
        for (int j = 0; j < this.authority.checks.size(); j++) {
            boolean successful = false;
            Check c = this.authority.checks.get(j);

            for(int k = 0; k < c.queries().size(); k++) {
                Set<Fact> res = world.query_rule(c.queries().get(k));
                if (!res.isEmpty()) {
                    successful = true;
                    break;
                }
            }

            if (!successful) {
                errors.add(new FailedCheck.FailedBlock(0, j, symbols.print_check(this.authority.checks.get(j))));
            }
        }

        for (int j = 0; j < verifier_checks.size(); j++) {
            boolean successful = false;
            Check c = verifier_checks.get(j);

            for(int k = 0; k < c.queries().size(); k++) {
                Set<Fact> res = world.query_rule(c.queries().get(k));
                if (!res.isEmpty()) {
                    successful = true;
                    break;
                }
            }

            if (!successful) {
                errors.add(new FailedCheck.FailedVerifier(j, symbols.print_check(verifier_checks.get(j))));
            }
        }

        for(int i = 0; i < this.blocks.size(); i++) {
            Block b = this.blocks.get(i);

            for (int j = 0; j < b.checks.size(); j++) {
                boolean successful = false;
                Check c = b.checks.get(j);

                for(int k = 0; k < c.queries().size(); k++) {
                    Set<Fact> res = world.query_rule(c.queries().get(k));
                    if (!res.isEmpty()) {
                        successful = true;
                        break;
                    }
                }

                if (!successful) {
                    errors.add(new FailedCheck.FailedBlock(b.index, j, symbols.print_check(b.checks.get(j))));
                }
            }
        }

        HashMap<String, Set<Fact>> query_results = new HashMap();
        for(String name: queries.keySet()) {
            Set<Fact> res = world.query_rule(queries.get(name));
            query_results.put(name, res);
        }

        if(errors.isEmpty()) {
            return Right(query_results);
        } else {
            return Left(new Error.FailedLogic(new LogicError.FailedChecks(errors)));
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
    public Either<Error, Biscuit> attenuate(final SecureRandom rng, final KeyPair keypair, Block block) {
        Either<Error, Biscuit> e = this.copy();

        if (e.isLeft()) {
            return Left(e.getLeft());
        }

        Biscuit copiedBiscuit = e.get();

        if(!Collections.disjoint(copiedBiscuit.symbols.symbols, block.symbols.symbols)) {
            return Left(new Error.SymbolTableOverlap());
        }

        if(block.index != 1 + this.blocks.size()) {
            return Left(new Error.InvalidBlockIndex(1 + copiedBiscuit.blocks.size(), block.index));
        }

        Either<Error.FormatError, SerializedBiscuit> containerRes = copiedBiscuit.container.get().append(rng, keypair, block);
        if(containerRes.isLeft()) {
            Error.FormatError error = containerRes.getLeft();
            return Left(error);
        }
        SerializedBiscuit container = containerRes.get();

        SymbolTable symbols = new SymbolTable(copiedBiscuit.symbols);
        for(String s: block.symbols.symbols) {
            symbols.add(s);
        }

        ArrayList<Block> blocks = new ArrayList<>();
        for(Block b: copiedBiscuit.blocks) {
            blocks.add(b);
        }
        blocks.add(block);

        return Right(new Biscuit(copiedBiscuit.authority, blocks, symbols, Option.some(container)));
    }

    public List<List<com.clevercloud.biscuit.datalog.Check>> checks() {
        ArrayList<List<com.clevercloud.biscuit.datalog.Check>> l = new ArrayList();
        l.add(new ArrayList(this.authority.checks));

        for(Block b: this.blocks) {
            l.add(new ArrayList<>(b.checks));
        }

        return l;
    }

    public List<byte[]> revocation_identifiers() {
        ArrayList<byte[]> l = new ArrayList<>();

        if(this.container.isEmpty()) {
            return l;
        } else {
            SerializedBiscuit b = this.container.get();

            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                digest.update(b.authority);
                digest.update(b.keys.get(0).compress().toByteArray());
                MessageDigest cloned = (MessageDigest)digest.clone();
                l.add(digest.digest());

                digest = cloned;

                for(int i = 0; i < b.blocks.size(); i++) {
                    byte[] block = b.blocks.get(i);
                    digest.update(block);
                    digest.update(b.keys.get(i+1).compress().toByteArray());
                    cloned = (MessageDigest)digest.clone();
                    l.add(digest.digest());

                    digest = cloned;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return l;
        }
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

    public Either<Error, Biscuit> copy() {
        return this
                .serialize()
                .map(Biscuit::from_bytes)
                .flatMap(e -> {
                    Either<Error, Biscuit> y = Either.narrow(e);
                    return y;
                });
    }
}
