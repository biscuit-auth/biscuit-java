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

import static io.vavr.API.Right;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.*;
import java.util.stream.Collectors;

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
     * <p>
     * this function uses the default symbol table
     *
     * @param root root private key
     * @return
     */
    public static com.clevercloud.biscuit.token.builder.Biscuit builder(final KeyPair root) {
        return new com.clevercloud.biscuit.token.builder.Biscuit(new SecureRandom(), root, default_symbol_table());
    }

    /**
     * Creates a token builder
     * <p>
     * this function uses the default symbol table
     *
     * @param rng  random number generator
     * @param root root private key
     * @return
     */
    public static com.clevercloud.biscuit.token.builder.Biscuit builder(final SecureRandom rng, final KeyPair root) {
        return new com.clevercloud.biscuit.token.builder.Biscuit(rng, root, default_symbol_table());
    }

    /**
     * Creates a token builder
     *
     * @param rng     random number generator
     * @param root    root private key
     * @param symbols symbol table
     * @return
     */
    public static com.clevercloud.biscuit.token.builder.Biscuit builder(final SecureRandom rng, final KeyPair root, SymbolTable symbols) {
        return new com.clevercloud.biscuit.token.builder.Biscuit(rng, root, symbols);
    }

    /**
     * Creates a token
     *
     * @param rng       random number generator
     * @param root      root private key
     * @param authority authority block
     * @return
     */
    static public Biscuit make(final SecureRandom rng, final KeyPair root, final SymbolTable symbols, final Block authority) throws Error.SymbolTableOverlap, Error.FormatError {
        if (!Collections.disjoint(symbols.symbols, authority.symbols.symbols)) {
            throw new Error.SymbolTableOverlap();
        }

        symbols.symbols.addAll(authority.symbols.symbols);
        ArrayList<Block> blocks = new ArrayList<>();

        KeyPair next = new KeyPair(rng);

        Either<Error.FormatError, SerializedBiscuit> container = SerializedBiscuit.make(root, authority, next);
        if (container.isLeft()) {
            Error.FormatError e = container.getLeft();
            throw e;
        } else {
            SerializedBiscuit s = container.get();
            List<byte[]> revocation_ids = s.revocation_identifiers();

            Option<SerializedBiscuit> c = Option.some(s);
            return new Biscuit(authority, blocks, symbols, c, revocation_ids);
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
     * <p>
     * This checks the signature, but does not verify that the first key is the root key,
     * to allow appending blocks without knowing about the root key.
     * <p>
     * The root key check is performed in the verify method
     * <p>
     * This method uses the default symbol table
     *
     * @param data
     * @return
     */
    @Deprecated
    static public Biscuit from_b64(String data, PublicKey root) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        return Biscuit.from_bytes(Base64.getUrlDecoder().decode(data), root);
    }

    /**
     * Deserializes a Biscuit token from a base64 url (RFC4648_URLSAFE) string
     * <p>
     * This checks the signature, but does not verify that the first key is the root key,
     * to allow appending blocks without knowing about the root key.
     * <p>
     * The root key check is performed in the verify method
     * <p>
     * This method uses the default symbol table
     *
     * @param data
     * @return Biscuit
     */
    static public Biscuit from_b64url(String data, PublicKey root) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        return Biscuit.from_bytes(Base64.getUrlDecoder().decode(data), root);
    }

    /**
     * Deserializes a Biscuit token from a byte array
     * <p>
     * This checks the signature, but does not verify that the first key is the root key,
     * to allow appending blocks without knowing about the root key.
     * <p>
     * The root key check is performed in the verify method
     * <p>
     * This method uses the default symbol table
     *
     * @param data
     * @return
     */
    static public Biscuit from_bytes(byte[] data, PublicKey root) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        return Biscuit.from_bytes_with_symbols(data, root, default_symbol_table());
    }

    /**
     * Deserializes a Biscuit token from a byte array
     * <p>
     * This checks the signature, but does not verify that the first key is the root key,
     * to allow appending blocks without knowing about the root key.
     * <p>
     * The root key check is performed in the verify method
     *
     * @param data
     * @return
     */
    static public Biscuit from_bytes_with_symbols(byte[] data, PublicKey root, SymbolTable symbols) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        //System.out.println("will deserialize and verify token");
        SerializedBiscuit ser = SerializedBiscuit.from_bytes(data, root);
        //System.out.println("deserialized token, will populate Biscuit structure");

        return Biscuit.from_serialize_biscuit(ser, symbols);
    }

    /**
     * Fills a Biscuit structure from a deserialized token
     *
     * @return
     */
    static Biscuit from_serialize_biscuit(SerializedBiscuit ser, SymbolTable symbols) throws Error {
        Either<Error.FormatError, Block> authRes = Block.from_bytes(ser.authority.block);
        if (authRes.isLeft()) {
            Error e = authRes.getLeft();
            throw e;
        }
        Block authority = authRes.get();

        ArrayList<Block> blocks = new ArrayList<>();
        for (SignedBlock bdata : ser.blocks) {
            Either<Error.FormatError, Block> blockRes = Block.from_bytes(bdata.block);
            if (blockRes.isLeft()) {
                Error e = blockRes.getLeft();
                throw e;
            }
            blocks.add(blockRes.get());
        }

        for (String s : authority.symbols.symbols) {
            symbols.add(s);
        }

        for (Block b : blocks) {
            for (String s : b.symbols.symbols) {
                symbols.add(s);
            }
        }

        List<byte[]> revocation_ids = ser.revocation_identifiers();

        return new Biscuit(authority, blocks, symbols, Option.some(ser), revocation_ids);
    }

    /**
     * Creates a authorizer for this token
     * <p>
     * This function checks that the root key is the one we expect
     *
     * @return
     */
    public Authorizer authorizer() throws Error.FailedLogic {
        return Authorizer.make(this);
    }

    /**
     * Serializes a token to a byte array
     *
     * @return
     */
    public byte[] serialize() throws Error.FormatError.SerializationError {
        if (this.container.isEmpty()) {
            throw new Error.FormatError.SerializationError("no internal container");
        }
        return this.container.get().serialize();
    }

    /**
     * Serializes a token to a base 64 String
     *
     * @return
     */
    @Deprecated
    public String serialize_b64() throws Error.FormatError.SerializationError {
        return Base64.getUrlEncoder().encodeToString(serialize());
    }

    /**
     * Serializes a token to base 64 url String using RFC4648_URLSAFE
     * @return String
     * @throws Error.FormatError.SerializationError
     */
    public String serialize_b64url() throws Error.FormatError.SerializationError {
        return Base64.getUrlEncoder().encodeToString(serialize());
    }

    public byte[] seal() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        if (this.container.isEmpty()) {
            throw new Error.FormatError.SerializationError("no internal container");
        }

        SerializedBiscuit ser = this.container.get();
        Either<Error, Void> res = ser.seal();
        if (res.isLeft()) {
            throw res.getLeft();
        }

        return ser.serialize();
    }

    public boolean is_sealed() {
        return this.container.isEmpty() ||
                this.container.get().proof.secretKey.isEmpty();
    }

    Either<Error, World> generate_world() {
        World world = new World();

        for (Fact fact : this.authority.facts) {
            world.add_fact(fact);
        }

        for (Rule rule : this.authority.rules) {
            world.add_rule(rule);
        }

        for (int i = 0; i < this.blocks.size(); i++) {
            Block b = this.blocks.get(i);

            for (Fact fact : b.facts) {
                world.add_fact(fact);
            }

            for (Rule rule : b.rules) {
                world.add_rule(rule);
            }
        }

        List<RevocationIdentifier> revocation_ids = this.revocation_identifiers();
        long rev = symbols.get("revocation_id").get();
        for (int i = 0; i < revocation_ids.size(); i++) {
            byte[] id = revocation_ids.get(i).getBytes();
            world.add_fact(new Fact(new Predicate(rev, Arrays.asList(new Term.Integer(i), new Term.Bytes(id)))));
        }

        return Right(world);
    }

    HashMap<String, Set<Fact>> check(SymbolTable symbols, List<Fact> ambient_facts, List<Rule> ambient_rules,
                                     List<Check> authorizer_checks, HashMap<String, Rule> queries) throws Error {
        Either<Error, World> wres = this.generate_world();

        if (wres.isLeft()) {
            Error e = wres.getLeft();
            throw e;
        }

        World world = wres.get();

        for (Fact fact : ambient_facts) {
            world.add_fact(fact);
        }

        for (Rule rule : ambient_rules) {
            world.add_rule(rule);
        }

        world.run(symbols);

        ArrayList<FailedCheck> errors = new ArrayList<>();
        for (int j = 0; j < this.authority.checks.size(); j++) {
            boolean successful = false;
            Check c = this.authority.checks.get(j);

            for (int k = 0; k < c.queries().size(); k++) {
                Set<Fact> res = world.query_rule(c.queries().get(k), symbols);
                if (!res.isEmpty()) {
                    successful = true;
                    break;
                }
            }

            if (!successful) {
                errors.add(new FailedCheck.FailedBlock(0, j, symbols.print_check(this.authority.checks.get(j))));
            }
        }

        for (int j = 0; j < authorizer_checks.size(); j++) {
            boolean successful = false;
            Check c = authorizer_checks.get(j);

            for (int k = 0; k < c.queries().size(); k++) {
                Set<Fact> res = world.query_rule(c.queries().get(k), symbols);
                if (!res.isEmpty()) {
                    successful = true;
                    break;
                }
            }

            if (!successful) {
                errors.add(new FailedCheck.FailedAuthorizer(j + 1, symbols.print_check(authorizer_checks.get(j))));
            }
        }

        for (int i = 0; i < this.blocks.size(); i++) {
            Block b = this.blocks.get(i);

            for (int j = 0; j < b.checks.size(); j++) {
                boolean successful = false;
                Check c = b.checks.get(j);

                for (int k = 0; k < c.queries().size(); k++) {
                    Set<Fact> res = world.query_rule(c.queries().get(k), symbols);
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

        HashMap<String, Set<Fact>> query_results = new HashMap<>();
        for (String name : queries.keySet()) {
            Set<Fact> res = world.query_rule(queries.get(name), symbols);
            query_results.put(name, res);
        }

        if (errors.isEmpty()) {
            return query_results;
        } else {
            throw new Error.FailedLogic(new LogicError.Unauthorized(new LogicError.MatchedPolicy.Allow(0), errors));
        }
    }

    /**
     * Creates a Block builder
     *
     * @return
     */
    public com.clevercloud.biscuit.token.builder.Block create_block() {
        return new com.clevercloud.biscuit.token.builder.Block(1 + this.blocks.size(), new SymbolTable(this.symbols));
    }

    /**
     * Generates a new token from an existing one and a new block
     *
     * @param block new block (should be generated from a Block builder)
     * @return
     */
    public Biscuit attenuate(com.clevercloud.biscuit.token.builder.Block block) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        SecureRandom rng = new SecureRandom();
        KeyPair keypair = new KeyPair(rng);
        return attenuate(rng, keypair, block.build());
    }

    /**
     * Generates a new token from an existing one and a new block
     *
     * @param rng     random number generator
     * @param keypair ephemeral key pair
     * @param block   new block (should be generated from a Block builder)
     * @return
     */
    public Biscuit attenuate(final SecureRandom rng, final KeyPair keypair, Block block) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        Biscuit copiedBiscuit = this.copy();

        if (!Collections.disjoint(copiedBiscuit.symbols.symbols, block.symbols.symbols)) {
            throw new Error.SymbolTableOverlap();
        }

        Either<Error.FormatError, SerializedBiscuit> containerRes = copiedBiscuit.container.get().append(keypair, block);
        if (containerRes.isLeft()) {
            Error.FormatError error = containerRes.getLeft();
            throw error;
        }
        SerializedBiscuit container = containerRes.get();

        SymbolTable symbols = new SymbolTable(copiedBiscuit.symbols);
        for (String s : block.symbols.symbols) {
            symbols.add(s);
        }

        ArrayList<Block> blocks = new ArrayList<>();
        for (Block b : copiedBiscuit.blocks) {
            blocks.add(b);
        }
        blocks.add(block);

        List<byte[]> revocation_ids = container.revocation_identifiers();

        return new Biscuit(copiedBiscuit.authority, blocks, symbols, Option.some(container), revocation_ids);
    }

    public List<List<com.clevercloud.biscuit.datalog.Check>> checks() {
        ArrayList<List<com.clevercloud.biscuit.datalog.Check>> l = new ArrayList<>();
        l.add(new ArrayList<>(this.authority.checks));

        for (Block b : this.blocks) {
            l.add(new ArrayList<>(b.checks));
        }

        return l;
    }

    public List<RevocationIdentifier> revocation_identifiers() {
        return this.revocation_ids.stream()
                .map(e -> RevocationIdentifier.from_bytes(e))
                .collect(Collectors.toList());
    }

    public List<Option<String>> context() {
        ArrayList<Option<String>> res = new ArrayList<>();
        if (this.authority.context.isEmpty()) {
            res.add(Option.none());
        } else {
            res.add(Option.some(this.authority.context));
        }

        for (Block b : this.blocks) {
            if (b.context.isEmpty()) {
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
        for (Block b : this.blocks) {
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

    public Biscuit copy() throws Error {
        return Biscuit.from_serialize_biscuit(this.container.get(), this.symbols);
    }
}
