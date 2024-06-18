package org.biscuitsec.biscuit.token;

import biscuit.format.schema.Schema;
import net.i2p.crypto.eddsa.EdDSAEngine;
import org.biscuitsec.biscuit.crypto.KeyDelegate;
import org.biscuitsec.biscuit.crypto.KeyPair;
import org.biscuitsec.biscuit.crypto.PublicKey;
import org.biscuitsec.biscuit.datalog.SymbolTable;
import org.biscuitsec.biscuit.error.Error;
import org.biscuitsec.biscuit.token.format.ExternalSignature;
import org.biscuitsec.biscuit.token.format.SerializedBiscuit;
import io.vavr.Tuple3;
import io.vavr.control.Either;
import io.vavr.control.Option;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.*;
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
        return new org.biscuitsec.biscuit.token.builder.Biscuit(new SecureRandom(), root);
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
        return new org.biscuitsec.biscuit.token.builder.Biscuit(rng, root);
    }

    /**
     * Creates a token builder
     *
     * @param rng     random number generator
     * @param root    root private key
     * @return
     */
    public static org.biscuitsec.biscuit.token.builder.Biscuit builder(final SecureRandom rng, final KeyPair root, final Option<Integer> root_key_id) {
        return new org.biscuitsec.biscuit.token.builder.Biscuit(rng, root, root_key_id);
    }

    /**
     * Creates a token
     *
     * @param rng       random number generator
     * @param root      root private key
     * @param authority authority block
     * @return Biscuit
     */
    public static Biscuit make(final SecureRandom rng, final KeyPair root, final Block authority) throws Error.FormatError {
        return Biscuit.make(rng, root, Option.none(), authority);
    }

    /**
     * Creates a token
     *
     * @param rng       random number generator
     * @param root      root private key
     * @param authority authority block
     * @return Biscuit
     */
    public static Biscuit make(final SecureRandom rng, final KeyPair root, final Integer root_key_id, final Block authority) throws Error.FormatError {
        return Biscuit.make(rng, root, Option.of(root_key_id), authority);
    }

    /**
     * Creates a token
     *
     * @param rng       random number generator
     * @param root      root private key
     * @param authority authority block
     * @return Biscuit
     */
    static Biscuit make(final SecureRandom rng, final KeyPair root, final Option<Integer> root_key_id, final Block authority) throws Error.FormatError {
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
            return new Biscuit(authority, blocks, authority.symbols, s, publicKeyToBlockId, revocation_ids, root_key_id);
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
        SymbolTable builderSymbols = new SymbolTable(this.symbols);
        return attenuate(rng, keypair, block.build(builderSymbols));
    }

    public Biscuit attenuate(final SecureRandom rng, final KeyPair keypair,org.biscuitsec.biscuit.token.builder.Block block) throws Error {
        SymbolTable builderSymbols = new SymbolTable(this.symbols);
        return attenuate(rng, keypair, block.build(builderSymbols));
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

        Either<Error.FormatError, SerializedBiscuit> containerRes = copiedBiscuit.serializedBiscuit.append(keypair, block, Option.none());
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
     * Generates a third party block request from a token
     */
    public ThirdPartyRequest thirdPartyRequest() {
        PublicKey previousKey;
        if(this.serializedBiscuit.blocks.isEmpty()) {
            previousKey = this.serializedBiscuit.authority.key;
        } else {
            previousKey = this.serializedBiscuit.blocks.get(this.serializedBiscuit.blocks.size() - 1).key;
        }

        List<PublicKey> publicKeys = new ArrayList<>(this.symbols.publicKeys());
        return new ThirdPartyRequest(previousKey, publicKeys);
    }

    /**
     * Generates a third party block request from a token
     */
    public Biscuit appendThirdPartyBlock(PublicKey externalKey, ThirdPartyBlock blockResponse)
            throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        KeyPair nextKeyPair = new KeyPair();

        Signature sgr = new EdDSAEngine(MessageDigest.getInstance(org.biscuitsec.biscuit.crypto.KeyPair.ed25519.getHashAlgorithm()));
        sgr.initVerify(externalKey.key);

        sgr.update(blockResponse.payload);
        ByteBuffer algo_buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        algo_buf.putInt(Integer.valueOf(Schema.PublicKey.Algorithm.Ed25519.getNumber()));
        algo_buf.flip();
        sgr.update(algo_buf);

        PublicKey previousKey;
        if(this.serializedBiscuit.blocks.isEmpty()) {
            previousKey = this.serializedBiscuit.authority.key;
        } else {
            previousKey = this.serializedBiscuit.blocks.get(this.serializedBiscuit.blocks.size() - 1).key;
        }
        sgr.update(previousKey.toBytes());
        if (!sgr.verify(blockResponse.signature)) {
            throw new Error.FormatError.Signature.InvalidSignature("signature error: Verification equation was not satisfied");
        }

        Either<Error.FormatError, Block> res = Block.from_bytes(blockResponse.payload, Option.some(externalKey));
        if(res.isLeft()) {
            throw res.getLeft();
        }

        Block block = res.get();

        ExternalSignature externalSignature = new ExternalSignature(externalKey, blockResponse.signature);

        Biscuit copiedBiscuit = this.copy();

        Either<Error.FormatError, SerializedBiscuit> containerRes = copiedBiscuit.serializedBiscuit.append(nextKeyPair, block, Option.some(externalSignature));
        if (containerRes.isLeft()) {
            throw containerRes.getLeft();
        }

        SerializedBiscuit container = containerRes.get();

        SymbolTable symbols = new SymbolTable(copiedBiscuit.symbols);

        ArrayList<Block> blocks = new ArrayList<>();
        for (Block b : copiedBiscuit.blocks) {
            blocks.add(b);
        }
        blocks.add(block);

        for(PublicKey pk: block.publicKeys) {
            symbols.insert(pk);
        }

        long pkIndex = symbols.insert(externalKey);
       // if (copiedBiscuit.publicKeyToBlockId.


        HashMap<Long, List<Long>> publicKeyToBlockId = new HashMap<>();
        publicKeyToBlockId.putAll(this.publicKeyToBlockId);
        if(publicKeyToBlockId.containsKey(pkIndex)) {
            publicKeyToBlockId.get(pkIndex).add((long)this.blocks.size()+1);
        } else {
            List<Long> list = new ArrayList<>();
            list.add((long)this.blocks.size()+1);
            publicKeyToBlockId.put(pkIndex, list);
        }

        List<byte[]> revocation_ids = container.revocation_identifiers();

        return new Biscuit(copiedBiscuit.authority, blocks, symbols, container, publicKeyToBlockId, revocation_ids);
    }

    /**
     * Prints a token's content
     */
    public String print() {
        StringBuilder s = new StringBuilder();
        s.append("Biscuit {\n\tsymbols: ");
        s.append(this.symbols.getAllSymbols());
        s.append("\n\tpublic keys: ");
        s.append(this.symbols.publicKeys());
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
