package org.biscuitsec.biscuit.token;

import org.biscuitsec.biscuit.crypto.KeyDelegate;
import org.biscuitsec.biscuit.crypto.KeyPair;
import org.biscuitsec.biscuit.crypto.PublicKey;
import org.biscuitsec.biscuit.datalog.SymbolTable;
import org.biscuitsec.biscuit.error.Error;
import org.biscuitsec.biscuit.token.format.SerializedBiscuit;
import io.vavr.Tuple3;
import io.vavr.control.Either;
import io.vavr.control.Option;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.*;

/**
 * Biscuit auth token
 */
public class Biscuit extends UnverifiedBiscuit {
    /**
     * Creates a token builder
     * <p>
     * this function uses the default symbol table
     *
     * @param root root private key
     * @return
     */
    public static org.biscuitsec.biscuit.token.builder.Biscuit builder(final KeyPair root) {
        return new org.biscuitsec.biscuit.token.builder.Biscuit(new SecureRandom(), root, default_symbol_table());
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
    public static org.biscuitsec.biscuit.token.builder.Biscuit builder(final SecureRandom rng, final KeyPair root) {
        return new org.biscuitsec.biscuit.token.builder.Biscuit(rng, root, default_symbol_table());
    }

    /**
     * Creates a token builder
     *
     * @param rng     random number generator
     * @param root    root private key
     * @param symbols symbol table
     * @return
     */
    public static org.biscuitsec.biscuit.token.builder.Biscuit builder(final SecureRandom rng, final KeyPair root, final Option<Integer> root_key_id, SymbolTable symbols) {
        return new org.biscuitsec.biscuit.token.builder.Biscuit(rng, root, root_key_id, symbols);
    }

    /**
     * Creates a token builder
     *
     * @param rng     random number generator
     * @param root    root private key
     * @param symbols symbol table
     * @return
     */
    public static org.biscuitsec.biscuit.token.builder.Biscuit builder(final SecureRandom rng, final KeyPair root, SymbolTable symbols) {
        return new org.biscuitsec.biscuit.token.builder.Biscuit(rng, root, symbols);
    }


    /**
     * Creates a token
     *
     * @param rng       random number generator
     * @param root      root private key
     * @param authority authority block
     * @return Biscuit
     */
    static public Biscuit make(final SecureRandom rng, final KeyPair root, final SymbolTable symbols, final Block authority) throws Error.SymbolTableOverlap, Error.FormatError {
        return Biscuit.make(rng, root, Option.none(), symbols, authority);
    }

    /**
     * Creates a token
     *
     * @param rng       random number generator
     * @param root      root private key
     * @param authority authority block
     * @return Biscuit
     */
    static public Biscuit make(final SecureRandom rng, final KeyPair root, final Integer root_key_id, final SymbolTable symbols, final Block authority) throws Error.SymbolTableOverlap, Error.FormatError {
        return Biscuit.make(rng, root, Option.of(root_key_id), symbols, authority);
    }

    /**
     * Creates a token
     *
     * @param rng       random number generator
     * @param root      root private key
     * @param authority authority block
     * @return Biscuit
     */
    static private Biscuit make(final SecureRandom rng, final KeyPair root, final Option<Integer> root_key_id,  final SymbolTable symbols, final Block authority) throws Error.SymbolTableOverlap, Error.FormatError {
        if (!Collections.disjoint(symbols.symbols, authority.symbols.symbols)) {
            throw new Error.SymbolTableOverlap();
        }

        symbols.symbols.addAll(authority.symbols.symbols);
        ArrayList<Block> blocks = new ArrayList<>();

        KeyPair next = new KeyPair(rng);

        Either<Error.FormatError, SerializedBiscuit> container = SerializedBiscuit.make(root, root_key_id, authority, next);
        if (container.isLeft()) {
            throw container.getLeft();
        } else {
            SerializedBiscuit s = container.get();
            List<byte[]> revocation_ids = s.revocation_identifiers();
            HashMap<Long, List<Long>> publicKeyToBlockId = new HashMap<>();

            Option<SerializedBiscuit> c = Option.some(s);
            return new Biscuit(authority, blocks, symbols, s, publicKeyToBlockId, revocation_ids, root_key_id);
        }
    }

    Biscuit(Block authority, List<Block> blocks, SymbolTable symbols, SerializedBiscuit serializedBiscuit,
            HashMap<Long, List<Long>> publicKeyToBlockId, List<byte[]> revocation_ids) {
        super(authority, blocks, symbols, serializedBiscuit, publicKeyToBlockId, revocation_ids);
    }

    Biscuit(Block authority, List<Block> blocks, SymbolTable symbols, SerializedBiscuit serializedBiscuit,
            HashMap<Long, List<Long>> publicKeyToBlockId, List<byte[]> revocation_ids, Option<Integer> root_key_id) {
        super(authority, blocks, symbols, serializedBiscuit, publicKeyToBlockId, revocation_ids, root_key_id);
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
    static public Biscuit from_b64url(String data, KeyDelegate delegate) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        return Biscuit.from_bytes(Base64.getUrlDecoder().decode(data), delegate);
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
        return from_bytes_with_symbols(data, root, default_symbol_table());
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
    static public Biscuit from_bytes(byte[] data, KeyDelegate delegate) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        return from_bytes_with_symbols(data, delegate, default_symbol_table());
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

        return Biscuit.from_serialized_biscuit(ser, symbols);
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
    static public Biscuit from_bytes_with_symbols(byte[] data, KeyDelegate delegate, SymbolTable symbols) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        //System.out.println("will deserialize and verify token");
        SerializedBiscuit ser = SerializedBiscuit.from_bytes(data, delegate);
        //System.out.println("deserialized token, will populate Biscuit structure");

        return Biscuit.from_serialized_biscuit(ser, symbols);
    }

    /**
     * Fills a Biscuit structure from a deserialized token
     *
     * @return
     */
    static Biscuit from_serialized_biscuit(SerializedBiscuit ser, SymbolTable symbols) throws Error {
        Tuple3<Block, ArrayList<Block>, HashMap<Long, List<Long>>> t = ser.extractBlocks(symbols);
        Block authority = t._1;
        ArrayList<Block> blocks = t._2;
        HashMap<Long, List<Long>> publicKeyToBlockId = t._3;

        List<byte[]> revocation_ids = ser.revocation_identifiers();

        return new Biscuit(authority, blocks, symbols, ser, publicKeyToBlockId, revocation_ids);
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
        return this.serializedBiscuit.serialize();
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
     *
     * @return String
     * @throws Error.FormatError.SerializationError
     */
    public String serialize_b64url() throws Error.FormatError.SerializationError {
        return Base64.getUrlEncoder().encodeToString(serialize());
    }

    /**
     * Generates a new token from an existing one and a new block
     *
     * @param block new block (should be generated from a Block builder)
     * @return
     */
    public Biscuit attenuate(org.biscuitsec.biscuit.token.builder.Block block) throws Error {
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
    public Biscuit attenuate(final SecureRandom rng, final KeyPair keypair, Block block) throws Error {
        Biscuit copiedBiscuit = this.copy();

        if (!Collections.disjoint(copiedBiscuit.symbols.symbols, block.symbols.symbols)) {
            throw new Error.SymbolTableOverlap();
        }

        Either<Error.FormatError, SerializedBiscuit> containerRes = copiedBiscuit.serializedBiscuit.append(keypair, block);
        if (containerRes.isLeft()) {
            throw containerRes.getLeft();
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

        HashMap<Long, List<Long>> publicKeyToBlockId = new HashMap<>();
        publicKeyToBlockId.putAll(this.publicKeyToBlockId);

        return new Biscuit(copiedBiscuit.authority, blocks, symbols, container, publicKeyToBlockId, revocation_ids);
    }

    /**
     * Prints a token's content
     */
    public String print() {
        StringBuilder s = new StringBuilder();
        s.append("Biscuit {\n\tsymbols: ");
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

    public Biscuit copy() throws Error {
        return Biscuit.from_serialized_biscuit(this.serializedBiscuit, this.symbols);
    }
}
