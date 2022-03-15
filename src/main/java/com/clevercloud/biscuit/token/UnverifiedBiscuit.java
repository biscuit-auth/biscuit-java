package com.clevercloud.biscuit.token;

import com.clevercloud.biscuit.crypto.KeyPair;
import com.clevercloud.biscuit.crypto.PublicKey;
import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.token.format.SerializedBiscuit;
import com.clevercloud.biscuit.token.format.SignedBlock;
import io.vavr.control.Either;
import io.vavr.control.Option;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UnverifiedBiscuit auth token. UnverifiedBiscuit means it's deserialized without checking signatures.
 */
public class UnverifiedBiscuit {
    final Block authority;
    final List<Block> blocks;
    final SymbolTable symbols;
    final Option<SerializedBiscuit> container;
    final List<byte[]> revocation_ids;

    UnverifiedBiscuit(Block authority, List<Block> blocks, SymbolTable symbols, Option<SerializedBiscuit> container, List<byte[]> revocation_ids) {
        this.authority = authority;
        this.blocks = blocks;
        this.symbols = symbols;
        this.container = container;
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

        return new UnverifiedBiscuit(authority, blocks, symbols, Option.some(ser), revocation_ids);
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

        return new UnverifiedBiscuit(copiedBiscuit.authority, blocks, symbols, Option.some(container), revocation_ids);
    }

    public List<RevocationIdentifier> revocation_identifiers() {
        return this.revocation_ids.stream()
                .map(RevocationIdentifier::from_bytes)
                .collect(Collectors.toList());
    }

    /**
     * Prints a token's content
     */
    public String print() {
        StringBuilder s = new StringBuilder();
        s.append("UnverifiedBiscuit {\n\tsymbols: ");
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
        return Biscuit.default_symbol_table();
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public UnverifiedBiscuit copy() throws Error {
        return UnverifiedBiscuit.from_bytes(this.serialize());
    }

    public Biscuit verify(PublicKey publicKey) throws Error, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        SerializedBiscuit serializedBiscuit = this.container.get();
        serializedBiscuit.verify(publicKey);
        return Biscuit.from_serialized_biscuit(serializedBiscuit, this.symbols);
    }
}
