package com.clevercloud.biscuit.token;

import com.clevercloud.biscuit.crypto.KeyPair;
import com.clevercloud.biscuit.crypto.PublicKey;
import com.clevercloud.biscuit.datalog.*;
import com.clevercloud.biscuit.error.FailedCheck;
import com.clevercloud.biscuit.error.LogicError;
import com.clevercloud.biscuit.token.format.SerializedBiscuit;
import com.clevercloud.biscuit.error.Error;

import com.clevercloud.biscuit.token.format.SignedBlock;
import io.vavr.control.Either;
import io.vavr.control.Option;
import static io.vavr.API.Left;
import static io.vavr.API.Right;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.*;

/**
 * Biscuit auth token
 */
public class Biscuit {
    final Block authority;
    final List<Block> blocks;
    final SymbolTable symbols;
    final Option<SerializedBiscuit> container;
    final List<byte[]> revocation_ids;


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

        symbols.symbols.addAll(authority.symbols.symbols);
        ArrayList<Block> blocks = new ArrayList<>();

        KeyPair next = new KeyPair(rng);

        Either<Error.FormatError, SerializedBiscuit> container = SerializedBiscuit.make(root, authority, next);
        if(container.isLeft()) {
            Error.FormatError e = container.getLeft();
            return Left(e);
        } else {
            SerializedBiscuit s = container.get();
            List<byte[]> revocation_ids = s.revocation_identifiers();

            Option<SerializedBiscuit> c = Option.some(s);
            return Right(new Biscuit(authority, blocks, symbols, c, revocation_ids));
        }
    }

    Biscuit(Block authority, List<Block> blocks, SymbolTable symbols, Option<SerializedBiscuit> container, List<byte[]> revocation_ids) {
        this.authority = authority;
        this.blocks = blocks;
        this.symbols = symbols;
        this.container = container;
        this.revocation_ids = revocation_ids;
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
    static public Either<Error, Biscuit> from_b64(String data, PublicKey root) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        return Biscuit.from_bytes(Base64.getUrlDecoder().decode(data), root);
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
    static public Either<Error, Biscuit> from_bytes(byte[] data, PublicKey root) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        return Biscuit.from_bytes_with_symbols(data, root, default_symbol_table());
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
    static public Either<Error, Biscuit> from_bytes_with_symbols(byte[] data, PublicKey root, SymbolTable symbols) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        //System.out.println("will deserialize and verify token");
        Either<Error, SerializedBiscuit> res = SerializedBiscuit.from_bytes(data, root);
        if(res.isLeft()) {
            Error e = res.getLeft();
            return Left(e);
        }

        SerializedBiscuit ser = res.get();
        //System.out.println("deserialized token, will populate Biscuit structure");

        return Biscuit.from_serialize_biscuit(ser, symbols);
    }

    /**
     * Fills a Biscuit structure from a deserialized token
     * @return
     */
    static Either<Error, Biscuit> from_serialize_biscuit(SerializedBiscuit ser, SymbolTable symbols) {
        Either<Error.FormatError, Block> authRes = Block.from_bytes(ser.authority.block);
        if(authRes.isLeft()){
            Error e = authRes.getLeft();
            return Left(e);
        }
        Block authority = authRes.get();

        ArrayList<Block> blocks = new ArrayList<>();
        for(SignedBlock bdata: ser.blocks) {
            Either<Error.FormatError, Block> blockRes = Block.from_bytes(bdata.block);
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

        List<byte[]> revocation_ids = ser.revocation_identifiers();

        return Right(new Biscuit(authority, blocks, symbols, Option.some(ser), revocation_ids));
    }

    static Either<Error, Biscuit> unsafe_from_bytes(byte[] data, SymbolTable symbols) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        Either<Error, SerializedBiscuit> res = SerializedBiscuit.unsafe_deserialize(data);
        if(res.isLeft()) {
            Error e = res.getLeft();
            return Left(e);
        }

        SerializedBiscuit ser = res.get();
        return Biscuit.from_serialize_biscuit(ser, symbols);
    }

    /**
     * Creates a verifier for this token
     *
     * This function checks that the root key is the one we expect
     * @return
     */
    public Either<Error, Verifier> verifier() {
        return Verifier.make(this);
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

    public Either<Error, byte[]> seal() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        if(this.container.isEmpty()) {
            return Left(new Error.FormatError.SerializationError("no internal container"));
        }

        SerializedBiscuit ser = this.container.get();
        Either<Error, Void> res = ser.seal();
        if(res.isLeft()) {
            return Left(res.getLeft());
        }

        return ser.serialize();
    }

    public boolean is_sealed() {
        return this.container.isEmpty() ||
                this.container.get().proof.secretKey.isEmpty();
    }

    Either<Error, World> generate_world() {
        World world = new World();
        long authority_index = symbols.get("authority").get();
        long ambient_index = symbols.get("ambient").get();

        for(Fact fact: this.authority.facts) {
            world.add_fact(fact);
        }

        for(Rule rule: this.authority.rules) {
            world.add_privileged_rule(rule);
        }

        for(int i = 0; i < this.blocks.size(); i++) {
            Block b = this.blocks.get(i);

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
            world.add_privileged_rule(rule);
        }

        HashSet<Long> restricted_symbols = new HashSet<>();
        restricted_symbols.add(symbols.get("authority").get());
        restricted_symbols.add(symbols.get("ambient").get());
        //System.out.println("world after adding ambient rules:\n"+symbols.print_world(world));
        world.run(restricted_symbols);
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
                errors.add(new FailedCheck.FailedVerifier(j + 1, symbols.print_check(verifier_checks.get(j))));
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
                    errors.add(new FailedCheck.FailedBlock(i + 1, j, symbols.print_check(b.checks.get(j))));
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
    public Either<Error, Biscuit> attenuate(final SecureRandom rng, final KeyPair keypair, Block block) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
       Either<Error, Biscuit> e = this.copy();

        if (e.isLeft()) {
            return Left(e.getLeft());
        }

        Biscuit copiedBiscuit = e.get();

        if(!Collections.disjoint(copiedBiscuit.symbols.symbols, block.symbols.symbols)) {
            return Left(new Error.SymbolTableOverlap());
        }

        Either<Error.FormatError, SerializedBiscuit> containerRes = copiedBiscuit.container.get().append(keypair, block);
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

        List<byte[]> revocation_ids = container.revocation_identifiers();

        return Right(new Biscuit(copiedBiscuit.authority, blocks, symbols, Option.some(container), revocation_ids));
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
        return this.revocation_ids;
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

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }


    public Either<Error, Biscuit> copy() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException  {
        Either<Error, byte[]> s = this.serialize();
        if (s.isLeft()) {
            return Left(s.getLeft());
        }

        return Biscuit.unsafe_from_bytes(s.get(), this.symbols);
    }
}
