package com.clevercloud.biscuit.token;

import com.clevercloud.biscuit.crypto.KeyPair;
import com.clevercloud.biscuit.crypto.PublicKey;
import com.clevercloud.biscuit.datalog.*;
import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.error.FailedCheck;
import com.clevercloud.biscuit.error.LogicError;
import com.clevercloud.biscuit.token.format.SerializedBiscuit;
import com.clevercloud.biscuit.token.format.SignedBlock;
import io.vavr.control.Either;
import io.vavr.control.Option;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.*;
import java.util.stream.Collectors;

import static io.vavr.API.Right;

/**
 * UnverifiedBiscuit auth token. UnverifiedBiscuit means it's deserialized without checking signatures.
 */
public class UnverifiedBiscuit {
    final Block authority;
    final List<Block> blocks;
    final SymbolTable symbols;
    final SerializedBiscuit serializedBiscuit;
    final List<byte[]> revocation_ids;

    UnverifiedBiscuit(Block authority, List<Block> blocks, SymbolTable symbols, SerializedBiscuit serializedBiscuit, List<byte[]> revocation_ids) {
        this.authority = authority;
        this.blocks = blocks;
        this.symbols = symbols;
        this.serializedBiscuit = serializedBiscuit;
        this.revocation_ids = revocation_ids;
    }

    /**
     * Deserializes a Biscuit token from a base64 url (RFC4648_URLSAFE) string
     * <p>
     * This method uses the default symbol table
     *
     * @param data
     * @return Biscuit
     */
    static public UnverifiedBiscuit from_b64url(String data) throws Error {
        return UnverifiedBiscuit.from_bytes(Base64.getUrlDecoder().decode(data));
    }

    /**
     * Deserializes a Biscuit token from a byte array
     * <p>
     * This method uses the default symbol table
     *
     * @param data
     * @return
     */
    static public UnverifiedBiscuit from_bytes(byte[] data) throws Error {
        return UnverifiedBiscuit.from_bytes_with_symbols(data, default_symbol_table());
    }

    /**
     * Deserializes a UnverifiedBiscuit from a byte array
     *
     * @param data
     * @return UnverifiedBiscuit
     */
    static public UnverifiedBiscuit from_bytes_with_symbols(byte[] data, SymbolTable symbols) throws Error {
        SerializedBiscuit ser = SerializedBiscuit.unsafe_deserialize(data);
        return UnverifiedBiscuit.from_serialized_biscuit(ser, symbols);
    }

    /**
     * Fills a UnverifiedBiscuit structure from a deserialized token
     *
     * @return UnverifiedBiscuit
     */
    static private UnverifiedBiscuit from_serialized_biscuit(SerializedBiscuit ser, SymbolTable symbols) throws Error {
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

        return new UnverifiedBiscuit(authority, blocks, symbols, ser, revocation_ids);
    }

    /**
     * Serializes a token to a byte array
     *
     * @return
     */
    public byte[] serialize() throws Error.FormatError.SerializationError {
        return this.serializedBiscuit.serialize();
    }

    /**
     * Serializes a token to base 64 url String using RFC4648_URLSAFE
     *
     * @return String
     * @throws Error.FormatError.SerializationError
     */
    public String serialize_b64url() throws Error.FormatError.SerializationError {
        return Base64.getUrlEncoder().encodeToString(serialize());
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
    public UnverifiedBiscuit attenuate(com.clevercloud.biscuit.token.builder.Block block) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
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
    public UnverifiedBiscuit attenuate(final SecureRandom rng, final KeyPair keypair, Block block) throws Error {
        UnverifiedBiscuit copiedBiscuit = this.copy();

        if (!Collections.disjoint(copiedBiscuit.symbols.symbols, block.symbols.symbols)) {
            throw new Error.SymbolTableOverlap();
        }

        Either<Error.FormatError, SerializedBiscuit> containerRes = copiedBiscuit.serializedBiscuit.append(keypair, block);
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

        return new UnverifiedBiscuit(copiedBiscuit.authority, blocks, symbols, container, revocation_ids);
    }

    public List<RevocationIdentifier> revocation_identifiers() {
        return this.revocation_ids.stream()
                .map(RevocationIdentifier::from_bytes)
                .collect(Collectors.toList());
    }

    public List<List<com.clevercloud.biscuit.datalog.Check>> checks() {
        ArrayList<List<com.clevercloud.biscuit.datalog.Check>> l = new ArrayList<>();
        l.add(new ArrayList<>(this.authority.checks));

        for (Block b : this.blocks) {
            l.add(new ArrayList<>(b.checks));
        }

        return l;
    }

    Either<Error, World> generate_world() {
        World world = new World();

        for (Fact fact : this.authority.facts) {
            world.add_fact(fact);
        }

        for (Rule rule : this.authority.rules) {
            world.add_rule(rule);
        }

        for (Block b : this.blocks) {
            for (Fact fact : b.facts) {
                world.add_fact(fact);
            }

            for (Rule rule : b.rules) {
                world.add_rule(rule);
            }
        }

        return Right(world);
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
     * Prints a token's content
     */
    public String print() {
        StringBuilder s = new StringBuilder();
        s.append("UnverifiedBiscuit {\n\tsymbols: ");
        s.append(this.symbols.getAllSymbols());
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

    public SymbolTable symbolTable() { return symbols; }
    public Block authorityBlock() { return authority; }
    public List<Block> blocks() { return blocks; }

    /**
     * Default symbols list
     */
    static public SymbolTable default_symbol_table() {
        return new SymbolTable();
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public UnverifiedBiscuit copy() throws Error {
        return UnverifiedBiscuit.from_bytes(this.serialize());
    }

    public Biscuit verify(PublicKey publicKey) throws Error, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        SerializedBiscuit serializedBiscuit = this.serializedBiscuit;
        serializedBiscuit.verify(publicKey);
        return Biscuit.from_serialized_biscuit(serializedBiscuit, this.symbols);
    }
}
