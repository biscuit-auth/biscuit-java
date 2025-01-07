package org.biscuitsec.biscuit.token;

import biscuit.format.schema.Schema;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.biscuitsec.biscuit.crypto.KeyDelegate;
import org.biscuitsec.biscuit.crypto.KeyPair;
import org.biscuitsec.biscuit.crypto.PublicKey;
import org.biscuitsec.biscuit.datalog.Check;
import org.biscuitsec.biscuit.datalog.SymbolTable;
import org.biscuitsec.biscuit.error.Error;
import org.biscuitsec.biscuit.token.format.ExternalSignature;
import org.biscuitsec.biscuit.token.format.SerializedBiscuit;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * UnverifiedBiscuit auth token. UnverifiedBiscuit means it's deserialized without checking signatures.
 */
@SuppressWarnings("JavadocDeclaration")
public class UnverifiedBiscuit {
    final Block authority;
    final List<Block> blocks;
    final SymbolTable symbolTable;
    final SerializedBiscuit serializedBiscuit;
    final List<byte[]> revocationIds;
    final Option<Integer> rootKeyId;

    UnverifiedBiscuit(Block authority,
                      List<Block> blocks,
                      SymbolTable symbolTable,
                      SerializedBiscuit serializedBiscuit,
                      List<byte[]> revocationIds) {
        this.authority = authority;
        this.blocks = blocks;
        this.symbolTable = symbolTable;
        this.serializedBiscuit = serializedBiscuit;
        this.revocationIds = revocationIds;
        this.rootKeyId = Option.none();
    }

    UnverifiedBiscuit(Block authority,
                      List<Block> blocks,
                      SymbolTable symbolTable,
                      SerializedBiscuit serializedBiscuit,
                      List<byte[]> revocationIds,
                      Option<Integer> rootKeyId) {
        this.authority = authority;
        this.blocks = blocks;
        this.symbolTable = symbolTable;
        this.serializedBiscuit = serializedBiscuit;
        this.revocationIds = revocationIds;
        this.rootKeyId = rootKeyId;
    }

    /**
     * Deserializes a Biscuit token from a base64 url (RFC4648_URLSAFE) string
     * <p>
     * This method uses the default symbol table
     *
     * @param data
     * @return Biscuit
     */
    static public UnverifiedBiscuit fromB64Url(String data) throws Error {
        return UnverifiedBiscuit.fromBytes(Base64.getUrlDecoder().decode(data));
    }

    /**
     * Deserializes a Biscuit token from a byte array
     * <p>
     * This method uses the default symbol table
     *
     * @param data
     * @return
     */
    static public UnverifiedBiscuit fromBytes(byte[] data) throws Error {
        return UnverifiedBiscuit.fromBytesWithSymbols(data, defaultSymbolTable());
    }

    /**
     * Deserializes a UnverifiedBiscuit from a byte array
     *
     * @param data
     * @return UnverifiedBiscuit
     */
    static public UnverifiedBiscuit fromBytesWithSymbols(byte[] data, SymbolTable symbolTable) throws Error {
        SerializedBiscuit ser = SerializedBiscuit.unsafeDeserialize(data);
        return UnverifiedBiscuit.fillUnverifiedBiscuitStructure(ser, symbolTable);
    }

    /**
     * Fills a UnverifiedBiscuit structure from a deserialized token
     *
     * @return UnverifiedBiscuit
     */
    static private UnverifiedBiscuit fillUnverifiedBiscuitStructure(SerializedBiscuit ser,
                                                                    SymbolTable symbolTable) throws Error {
        Tuple2<Block, ArrayList<Block>> t = ser.extractBlocks(symbolTable);
        Block authority = t._1;
        ArrayList<Block> blocks = t._2;

        List<byte[]> revocationIds = ser.revocationIdentifiers();

        return new UnverifiedBiscuit(authority, blocks, symbolTable, ser, revocationIds);
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
    public String serializeB64Url() throws Error.FormatError.SerializationError {
        return Base64.getUrlEncoder().encodeToString(serialize());
    }

    /**
     * Creates a Block builder
     *
     * @return
     */
    public org.biscuitsec.biscuit.token.builder.Block createBlock() {
        return new org.biscuitsec.biscuit.token.builder.Block();
    }

    /**
     * Generates a new token from an existing one and a new block
     *
     * @param block new block (should be generated from a Block builder)
     * @return
     */
    public UnverifiedBiscuit attenuate(org.biscuitsec.biscuit.token.builder.Block block) throws Error {
        SecureRandom rng = new SecureRandom();
        KeyPair keypair = KeyPair.generate(Schema.PublicKey.Algorithm.Ed25519, rng);
        SymbolTable builderSymbolTable = new SymbolTable(this.symbolTable);
        return attenuate(rng, keypair, block.build(builderSymbolTable));
    }

    public UnverifiedBiscuit attenuate(final SecureRandom rng,
                                       final KeyPair keypair,
                                       org.biscuitsec.biscuit.token.builder.Block block) throws Error {
        SymbolTable builderSymbolTable = new SymbolTable(this.symbolTable);
        return attenuate(rng, keypair, block.build(builderSymbolTable));
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

        checkSymbolTableOverlap(copiedBiscuit, block);

        Either<Error.FormatError, SerializedBiscuit> containerRes = copiedBiscuit.serializedBiscuit.append(keypair, block, Option.none());
        containerResIsLeft(containerRes);
        SerializedBiscuit container = containerRes.get();

        SymbolTable symbolTable = new SymbolTable(copiedBiscuit.symbolTable);
        for (String s : block.symbolTable.symbols) {
            symbolTable.add(s);
        }

        ArrayList<Block> blocks = new ArrayList<>();
        addCopiedBiscuitBlocks(copiedBiscuit);
        blocks.add(block);

        List<byte[]> revocationIds = container.revocationIdentifiers();

        return new UnverifiedBiscuit(copiedBiscuit.authority, blocks, symbolTable, container, revocationIds);
    }
    //FIXME: attenuate 3rd Party

    protected void checkSymbolTableOverlap(UnverifiedBiscuit copiedBiscuit, Block block) throws Error {
        if (!Collections.disjoint(copiedBiscuit.symbolTable.symbols, block.symbolTable.symbols)) {
            throw new Error.SymbolTableOverlap();
        }
    }

    protected void containerResIsLeft(Either<Error.FormatError, SerializedBiscuit> containerRes) throws Error {
        if (containerRes.isLeft()) {
            throw containerRes.getLeft();
        }
    }

    protected void addCopiedBiscuitBlocks(UnverifiedBiscuit copiedBiscuit) {
        blocks.addAll(copiedBiscuit.blocks);
    }

    public List<RevocationIdentifier> revocationIdentifiers() {
        return this.revocationIds.stream()
                .map(RevocationIdentifier::fromBytes)
                .collect(toList());
    }

    @SuppressWarnings("unused")
    public List<List<Check>> checks() {
        // Should this unused method be deprecated?
        ArrayList<List<Check>> l = new ArrayList<>();
        l.add(new ArrayList<>(this.authority.checks));

        for (Block b : this.blocks) {
            l.add(new ArrayList<>(b.checks));
        }

        return l;
    }

    @SuppressWarnings("unused")
    public List<Option<String>> context() {
        // Should this unused method be deprecated?
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

    @SuppressWarnings("unused")
    public Option<Integer> rootKeyId() {
        // Should this unused method be deprecated?
        return this.rootKeyId;
    }

    /**
     * Generates a third party block request from a token
     */
    public ThirdPartyBlockRequest thirdPartyRequest() {
        PublicKey previousKey;
        if (this.serializedBiscuit.blocks.isEmpty()) {
            previousKey = this.serializedBiscuit.authority.key;
        } else {
            previousKey = this.serializedBiscuit.blocks.get(this.serializedBiscuit.blocks.size() - 1).key;
        }

        return new ThirdPartyBlockRequest(previousKey);
    }


    /**
     * Generates a third party block request from a token
     */
    public UnverifiedBiscuit appendThirdPartyBlock(PublicKey externalKey, ThirdPartyBlockContents blockResponse)
            throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        KeyPair nextKeyPair = KeyPair.generate(Schema.PublicKey.Algorithm.Ed25519);

        Signature sgr = KeyPair.generateSignature(externalKey.algorithm);
        sgr.initVerify(externalKey.key);

        sgr.update(blockResponse.payload);
        ByteBuffer algo_buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        algo_buf.putInt(Schema.PublicKey.Algorithm.Ed25519.getNumber());
        algo_buf.flip();
        sgr.update(algo_buf);

        PublicKey previousKey;
        if (this.serializedBiscuit.blocks.isEmpty()) {
            previousKey = this.serializedBiscuit.authority.key;
        } else {
            previousKey = this.serializedBiscuit.blocks.get(this.serializedBiscuit.blocks.size() - 1).key;
        }
        sgr.update(previousKey.toBytes());
        if (!sgr.verify(blockResponse.signature)) {
            throw new Error.FormatError.Signature.InvalidSignature("signature error: Verification equation was not satisfied");
        }

        Either<Error.FormatError, Block> res = Block.fromBytes(blockResponse.payload, Option.some(externalKey));
        if (res.isLeft()) {
            throw res.getLeft();
        }

        Block block = res.get();

        ExternalSignature externalSignature = new ExternalSignature(externalKey, blockResponse.signature);

        UnverifiedBiscuit copiedBiscuit = this.copy();

        Either<Error.FormatError, SerializedBiscuit> containerRes =
                copiedBiscuit.serializedBiscuit.append(nextKeyPair, block, Option.some(externalSignature));
        if (containerRes.isLeft()) {
            throw containerRes.getLeft();
        }

        SerializedBiscuit container = containerRes.get();

        SymbolTable symbolTable = new SymbolTable(copiedBiscuit.symbolTable);

        ArrayList<Block> blocks = new ArrayList<>();
        addCopiedBiscuitBlocks(copiedBiscuit);
        blocks.add(block);

        List<byte[]> revocationIds = container.revocationIdentifiers();
        return new UnverifiedBiscuit(copiedBiscuit.authority, blocks, symbolTable, container, revocationIds);
    }

    /**
     * Prints a token's content
     */
    public String print() {
        StringBuilder s = new StringBuilder();
        s.append("UnverifiedBiscuit {\n\tsymbols: ");
        s.append(this.symbolTable.getAllSymbols());
        s.append("\n\tauthority: ");
        s.append(this.authority.print(this.symbolTable));
        s.append("\n\tblocks: [\n");
        for (Block b : this.blocks) {
            s.append("\t\t");
            s.append(b.print(this.symbolTable));
            s.append("\n");
        }
        s.append("\t]\n}");

        return s.toString();
    }

    /**
     * Default symbols list
     */
    static public SymbolTable defaultSymbolTable() {
        return new SymbolTable();
    }

    public UnverifiedBiscuit copy() throws Error {
        return UnverifiedBiscuit.fromBytes(this.serialize());
    }

    public Biscuit verify(PublicKey publicKey)
            throws Error, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        SerializedBiscuit serializedBiscuit = this.serializedBiscuit;
        serializedBiscuit.verify(publicKey);
        return Biscuit.fromSerializedBiscuit(serializedBiscuit, this.symbolTable);
    }

    @SuppressWarnings("unused")
    public Biscuit verify(KeyDelegate delegate)
            throws Error, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        SerializedBiscuit serializedBiscuit = this.serializedBiscuit;


        Option<PublicKey> root = delegate.rootKey(rootKeyId);
        if (root.isEmpty()) {
            throw new InvalidKeyException("unknown root key id");
        }

        serializedBiscuit.verify(root.get());
        return Biscuit.fromSerializedBiscuit(serializedBiscuit, this.symbolTable);
    }
}
