package org.biscuitsec.biscuit.token.format;

import biscuit.format.schema.Schema;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Option;
import net.i2p.crypto.eddsa.EdDSAEngine;
import org.biscuitsec.biscuit.crypto.KeyDelegate;
import org.biscuitsec.biscuit.crypto.KeyPair;
import org.biscuitsec.biscuit.datalog.SymbolTable;
import org.biscuitsec.biscuit.error.Error;
import org.biscuitsec.biscuit.token.Block;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.*;
import java.util.ArrayList;
import java.util.List;

import static io.vavr.API.Left;
import static io.vavr.API.Right;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.biscuitsec.biscuit.crypto.KeyPair.ed25519;

/**
 * Intermediate representation of a token before full serialization
 */
@SuppressWarnings("JavadocDeclaration")
public class SerializedBiscuit {

    public static int MIN_SCHEMA_VERSION = 3;
    public static int MAX_SCHEMA_VERSION = 5;
    public final SignedBlock authority;
    public final List<SignedBlock> blocks;
    public final Proof proof;
    public Option<Integer> rootKeyId;

    SerializedBiscuit(SignedBlock authority, List<SignedBlock> blocks, Proof proof) {
        this.authority = authority;
        this.blocks = blocks;
        this.proof = proof;
        this.rootKeyId = Option.none();
    }

    SerializedBiscuit(SignedBlock authority, List<SignedBlock> blocks, Proof proof, Option<Integer> rootKeyId) {
        this.authority = authority;
        this.blocks = blocks;
        this.proof = proof;
        this.rootKeyId = rootKeyId;
    }

    public Either<Error.FormatError, SerializedBiscuit> append(final org.biscuitsec.biscuit.crypto.KeyPair next,
                                                               final Block newBlock,
                                                               Option<ExternalSignature> externalSignature) {
        if (this.proof.secretKey.isEmpty()) {
            return Left(new Error.FormatError.SerializationError("the token is sealed"));
        }

        Schema.Block b = newBlock.serialize();
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            b.writeTo(stream);

            byte[] block = stream.toByteArray();
            org.biscuitsec.biscuit.crypto.PublicKey nextKey = next.publicKey();
            ByteBuffer algo_buf = ByteBuffer.allocate(4).order(LITTLE_ENDIAN);
            algo_buf.putInt(nextKey.algorithm.getNumber());
            algo_buf.flip();

            Signature sgr = new EdDSAEngine(MessageDigest.getInstance(ed25519.getHashAlgorithm()));
            sgr.initSign(this.proof.secretKey.get().privateKey);
            sgr.update(block);
            if (externalSignature.isDefined()) {
                sgr.update(externalSignature.get().signature);
            }
            sgr.update(algo_buf);
            sgr.update(nextKey.toBytes());
            byte[] signature = sgr.sign();

            SignedBlock signedBlock = new SignedBlock(block, nextKey, signature, externalSignature);

            ArrayList<SignedBlock> blocks = new ArrayList<>(this.blocks);
            blocks.add(signedBlock);

            Proof proof = new Proof(next);

            return Right(new SerializedBiscuit(this.authority, blocks, proof, rootKeyId));
        } catch (IOException | NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            return Left(new Error.FormatError.SerializationError(e.toString()));
        }
    }

    public Tuple2<Block, ArrayList<Block>> extractBlocks(SymbolTable symbols) throws Error {
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        ArrayList<Option<org.biscuitsec.biscuit.crypto.PublicKey>> blockExternalKeys = new ArrayList<>();
        Either<Error.FormatError, Block> authRes = Block.from_bytes(this.authority.block, Option.none());
        if (authRes.isLeft()) {
            throw authRes.getLeft();
        }
        Block authority = authRes.get();
        for (org.biscuitsec.biscuit.crypto.PublicKey pk : authority.publicKeys()) {
            symbols.insert(pk);
        }
        blockExternalKeys.add(Option.none());

        for (String s : authority.symbols().symbols) {
            symbols.add(s);
        }

        ArrayList<Block> blocks = new ArrayList<>();
        for (SignedBlock blockData : this.blocks) {
            Option<org.biscuitsec.biscuit.crypto.PublicKey> externalKey = Option.none();
            if (blockData.externalSignature.isDefined()) {
                externalKey = Option.some(blockData.externalSignature.get().key);
            }
            Either<Error.FormatError, Block> blockRes = Block.from_bytes(blockData.block, externalKey);
            if (blockRes.isLeft()) {
                throw blockRes.getLeft();
            }
            Block block = blockRes.get();

            // blocks with external signatures keep their own symbol table
            if (blockData.externalSignature.isDefined()) {
                //symbols.insert(blockData.externalSignature.get().key);
                blockExternalKeys.add(Option.some(blockData.externalSignature.get().key));
            } else {
                blockExternalKeys.add(Option.none());
                for (String s : block.symbols().symbols) {
                    symbols.add(s);
                }
                for (org.biscuitsec.biscuit.crypto.PublicKey pk : block.publicKeys()) {
                    symbols.insert(pk);
                }
            }

            blocks.add(block);
        }

        return new Tuple2<>(authority, blocks);
    }

    /**
     * Deserializes a SerializedBiscuit from a byte array
     *
     * @param slice
     * @return
     */
    static public SerializedBiscuit fromBytes(byte[] slice, org.biscuitsec.biscuit.crypto.PublicKey root)
            throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        try {
            Schema.Biscuit data = Schema.Biscuit.parseFrom(slice);

            return fromBytesInner(data, root);
        } catch (InvalidProtocolBufferException e) {
            throw new Error.FormatError.DeserializationError(e.toString());
        }
    }

    /**
     * Deserializes a SerializedBiscuit from a byte array
     *
     * @param slice
     * @return
     */
    static public SerializedBiscuit fromBytes(byte[] slice, KeyDelegate delegate)
            throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        try {
            Schema.Biscuit data = Schema.Biscuit.parseFrom(slice);

            Option<Integer> rootKeyId = Option.none();
            if (data.hasRootKeyId()) {
                rootKeyId = Option.some(data.getRootKeyId());
            }

            Option<org.biscuitsec.biscuit.crypto.PublicKey> root = delegate.rootKey(rootKeyId);
            if (root.isEmpty()) {
                throw new InvalidKeyException("unknown root key id");
            }

            return fromBytesInner(data, root.get());
        } catch (InvalidProtocolBufferException e) {
            throw new Error.FormatError.DeserializationError(e.toString());
        }
    }

    @SuppressWarnings("unused")
    static public Either<Error.FormatError, SerializedBiscuit> make(final org.biscuitsec.biscuit.crypto.KeyPair root,
                                                                    final Block authority,
                                                                    final org.biscuitsec.biscuit.crypto.KeyPair next) {
        return make(root, Option.none(), authority, next);
    }

    static public Either<Error.FormatError, SerializedBiscuit> make(final org.biscuitsec.biscuit.crypto.KeyPair root,
                                                                    final Option<Integer> rootKeyId,
                                                                    final Block authority,
                                                                    final org.biscuitsec.biscuit.crypto.KeyPair next) {
        Schema.Block b = authority.serialize();
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            b.writeTo(stream);
            byte[] block = stream.toByteArray();
            org.biscuitsec.biscuit.crypto.PublicKey next_key = next.publicKey();
            ByteBuffer algoBuf = ByteBuffer.allocate(4).order(LITTLE_ENDIAN);
            algoBuf.putInt(next_key.algorithm.getNumber());
            algoBuf.flip();

            Signature sgr = KeyPair.generateSignature(root.publicKey().algorithm);
            sgr.initSign(root.privateKey);
            sgr.update(block);
            sgr.update(algoBuf);
            sgr.update(next_key.toBytes());
            byte[] signature = sgr.sign();

            SignedBlock signedBlock = new SignedBlock(block, next_key, signature, Option.none());
            Proof proof = new Proof(next);

            return Right(new SerializedBiscuit(signedBlock, new ArrayList<>(), proof, rootKeyId));
        } catch (IOException | NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            return Left(new Error.FormatError.SerializationError(e.toString()));
        }
    }

    public List<byte[]> revocationIdentifiers() {
        ArrayList<byte[]> l = new ArrayList<>();
        l.add(this.authority.signature);

        for (SignedBlock block : this.blocks) {
            l.add(block.signature);
        }
        return l;
    }

    @SuppressWarnings("unused")
    public Either<Error, Void> seal() throws InvalidKeyException, NoSuchAlgorithmException, SignatureException {
        if (this.proof.secretKey.isEmpty()) {
            return Left(new Error.Sealed());
        }

        SignedBlock block;
        if (this.blocks.isEmpty()) {
            block = this.authority;
        } else {
            block = this.blocks.get(this.blocks.size() - 1);
        }

        Signature sgr = new EdDSAEngine(MessageDigest.getInstance(ed25519.getHashAlgorithm()));
        ByteBuffer algoBuf = ByteBuffer.allocate(4).order(LITTLE_ENDIAN);
        algoBuf.putInt(block.key.algorithm.getNumber());
        algoBuf.flip();

        sgr.initSign(this.proof.secretKey.get().privateKey);
        sgr.update(block.block);
        sgr.update(algoBuf);
        sgr.update(block.key.toBytes());
        sgr.update(block.signature);

        byte[] signature = sgr.sign();

        this.proof.secretKey = Option.none();
        this.proof.signature = Option.some(signature);

        return Right(null);
    }

    /**
     * Serializes a SerializedBiscuit to a byte array
     *
     * @return
     */
    public byte[] serialize() throws Error.FormatError.SerializationError {
        Schema.Biscuit.Builder biscuitBuilder = Schema.Biscuit.newBuilder();
        Schema.SignedBlock.Builder authorityBuilder = Schema.SignedBlock.newBuilder();
        {
            SignedBlock block = this.authority;
            authorityBuilder.setBlock(ByteString.copyFrom(block.block));
            authorityBuilder.setNextKey(block.key.serialize());
            authorityBuilder.setSignature(ByteString.copyFrom(block.signature));
        }
        biscuitBuilder.setAuthority(authorityBuilder.build());

        for (SignedBlock block : this.blocks) {
            Schema.SignedBlock.Builder blockBuilder = Schema.SignedBlock.newBuilder();
            blockBuilder.setBlock(ByteString.copyFrom(block.block));
            blockBuilder.setNextKey(block.key.serialize());
            blockBuilder.setSignature(ByteString.copyFrom(block.signature));

            if (block.externalSignature.isDefined()) {
                ExternalSignature externalSignature = block.externalSignature.get();
                Schema.ExternalSignature.Builder externalSignatureBuilder = Schema.ExternalSignature.newBuilder();
                externalSignatureBuilder.setPublicKey(externalSignature.key.serialize());
                externalSignatureBuilder.setSignature(ByteString.copyFrom(externalSignature.signature));
                blockBuilder.setExternalSignature(externalSignatureBuilder.build());
            }

            biscuitBuilder.addBlocks(blockBuilder.build());
        }

        Schema.Proof.Builder proofBuilder = Schema.Proof.newBuilder();
        if (!this.proof.secretKey.isEmpty()) {
            proofBuilder.setNextSecret(ByteString.copyFrom(this.proof.secretKey.get().toBytes()));
        } else {
            proofBuilder.setFinalSignature(ByteString.copyFrom(this.proof.signature.get()));
        }

        biscuitBuilder.setProof(proofBuilder.build());
        if (!this.rootKeyId.isEmpty()) {
            biscuitBuilder.setRootKeyId(this.rootKeyId.get());
        }

        Schema.Biscuit biscuit = biscuitBuilder.build();

        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            biscuit.writeTo(stream);
            return stream.toByteArray();
        } catch (IOException e) {
            throw new Error.FormatError.SerializationError(e.toString());
        }
    }

    /**
     * Warning: this deserializes without verifying the signature
     *
     * @param slice
     * @return SerializedBiscuit
     * @throws Error.FormatError.DeserializationError
     */
    static public SerializedBiscuit unsafeDeserialize(byte[] slice) throws Error.FormatError.DeserializationError {
        try {
            Schema.Biscuit data = Schema.Biscuit.parseFrom(slice);
            return SerializedBiscuit.deserialize(data);
        } catch (InvalidProtocolBufferException e) {
            throw new Error.FormatError.DeserializationError(e.toString());
        }
    }

    public Either<Error, Void> verify(org.biscuitsec.biscuit.crypto.PublicKey root)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        org.biscuitsec.biscuit.crypto.PublicKey currentKey = root;
        ByteBuffer algoBuf = ByteBuffer.allocate(4).order(LITTLE_ENDIAN);
        {
            Either<Error, org.biscuitsec.biscuit.crypto.PublicKey> res = verifyBlockSignature(this.authority, currentKey);
            if (res.isRight()) {
                currentKey = res.get();
            } else {
                return Left(res.getLeft());
            }
        }

        for (SignedBlock b : this.blocks) {
            Either<Error, org.biscuitsec.biscuit.crypto.PublicKey> res = verifyBlockSignature(b, currentKey);
            if (res.isRight()) {
                currentKey = res.get();
            } else {
                return Left(res.getLeft());
            }
        }

        //System.out.println("signatures verified, checking proof");

        if (!this.proof.secretKey.isEmpty()) {
            //System.out.println("checking secret key");
            //System.out.println("current key: "+currentKey.toHex());
            //System.out.println("key from proof: "+this.proof.secretKey.get().publicKey().toHex());
            if (this.proof.secretKey.get().publicKey().equals(currentKey)) {
                //System.out.println("public keys are equal");

                return Right(null);
            } else {
                //System.out.println("public keys are not equal");

                return Left(new Error.FormatError.Signature.InvalidSignature("signature error: Verification equation was not satisfied"));
            }
        } else {
            //System.out.println("checking final signature");

            byte[] finalSignature = this.proof.signature.get();

            SignedBlock b;
            if (this.blocks.isEmpty()) {
                b = this.authority;
            } else {
                b = this.blocks.get(this.blocks.size() - 1);
            }

            byte[] block = b.block;
            org.biscuitsec.biscuit.crypto.PublicKey nextKey = b.key;
            byte[] signature = b.signature;
            algoBuf.clear();
            algoBuf.putInt(nextKey.algorithm.getNumber());
            algoBuf.flip();

            Signature sgr = new EdDSAEngine(MessageDigest.getInstance(ed25519.getHashAlgorithm()));

            sgr.initVerify(currentKey.key);
            sgr.update(block);
            sgr.update(algoBuf);
            sgr.update(nextKey.toBytes());
            sgr.update(signature);

            if (sgr.verify(finalSignature)) {
                return Right(null);
            } else {
                return Left(new Error.FormatError.Signature.SealedSignature());
            }

        }
    }

    static SerializedBiscuit fromBytesInner(Schema.Biscuit data, org.biscuitsec.biscuit.crypto.PublicKey root)
            throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        SerializedBiscuit b = SerializedBiscuit.deserialize(data);
        if (data.hasRootKeyId()) {
            b.rootKeyId = Option.some(data.getRootKeyId());
        }

        Either<Error, Void> res = b.verify(root);
        if (res.isLeft()) {
            throw res.getLeft();
        } else {
            return b;
        }

    }

    static Either<Error, org.biscuitsec.biscuit.crypto.PublicKey> verifyBlockSignature(SignedBlock signedBlock,
                                                                                       org.biscuitsec.biscuit.crypto.PublicKey publicKey)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        byte[] block = signedBlock.block;
        org.biscuitsec.biscuit.crypto.PublicKey nextKey = signedBlock.key;
        byte[] signature = signedBlock.signature;
        if (signature.length != 64) {
            return Either.left(new Error.FormatError.Signature.InvalidSignatureSize(signature.length));
        }
        ByteBuffer algoBuf = ByteBuffer.allocate(4).order(LITTLE_ENDIAN);
        algoBuf.putInt(nextKey.algorithm.getNumber());
        algoBuf.flip();

        Signature sgr = KeyPair.generateSignature(publicKey.algorithm);

        sgr.initVerify(publicKey.key);
        sgr.update(block);
        if (signedBlock.externalSignature.isDefined()) {
            sgr.update(signedBlock.externalSignature.get().signature);
        }
        sgr.update(algoBuf);
        sgr.update(nextKey.toBytes());
        if (!sgr.verify(signature)) {
            return Left(new Error.FormatError.Signature.InvalidSignature("signature error: Verification equation was not satisfied"));
        }

        if (signedBlock.externalSignature.isDefined()) {
            ByteBuffer algoBuf2 = ByteBuffer.allocate(4).order(LITTLE_ENDIAN);
            algoBuf2.putInt(publicKey.algorithm.getNumber());
            algoBuf2.flip();

            Signature sgr2 = new EdDSAEngine(MessageDigest.getInstance(ed25519.getHashAlgorithm()));
            sgr2.initVerify(signedBlock.externalSignature.get().key.key);
            sgr2.update(block);
            sgr2.update(algoBuf2);
            sgr2.update(publicKey.toBytes());

            if (!sgr2.verify(signedBlock.externalSignature.get().signature)) {
                return Left(new Error.FormatError.Signature.InvalidSignature("external signature error: Verification equation was not satisfied"));
            }
        }

        return Right(nextKey);
    }

    /**
     * Warning: this deserializes without verifying the signature
     *
     * @param data
     * @return SerializedBiscuit
     * @throws Error.FormatError.DeserializationError
     */
    static private SerializedBiscuit deserialize(Schema.Biscuit data) throws Error.FormatError.DeserializationError {
        if (data.getAuthority().hasExternalSignature()) {
            throw new Error.FormatError.DeserializationError("the authority block must not contain an external signature");
        }

        SignedBlock authority = new SignedBlock(
                data.getAuthority().getBlock().toByteArray(),
                org.biscuitsec.biscuit.crypto.PublicKey.deserialize(data.getAuthority().getNextKey()),
                data.getAuthority().getSignature().toByteArray(),
                Option.none()
        );

        ArrayList<SignedBlock> blocks = new ArrayList<>();
        for (Schema.SignedBlock block : data.getBlocksList()) {
            Option<ExternalSignature> external = Option.none();
            if (block.hasExternalSignature() && block.getExternalSignature().hasPublicKey()
                    && block.getExternalSignature().hasSignature()) {
                Schema.ExternalSignature ex = block.getExternalSignature();
                external = Option.some(new ExternalSignature(
                        org.biscuitsec.biscuit.crypto.PublicKey.deserialize(ex.getPublicKey()),
                        ex.getSignature().toByteArray()));

            }
            blocks.add(new SignedBlock(
                    block.getBlock().toByteArray(),
                    org.biscuitsec.biscuit.crypto.PublicKey.deserialize(block.getNextKey()),
                    block.getSignature().toByteArray(),
                    external
            ));
        }

        Option<org.biscuitsec.biscuit.crypto.KeyPair> secretKey = Option.none();
        if (data.getProof().hasNextSecret()) {
            secretKey = Option.some(new org.biscuitsec.biscuit.crypto.KeyPair(data.getProof().getNextSecret().toByteArray()));
        }

        Option<byte[]> signature = Option.none();
        if (data.getProof().hasFinalSignature()) {
            signature = Option.some(data.getProof().getFinalSignature().toByteArray());
        }

        if (secretKey.isEmpty() && signature.isEmpty()) {
            throw new Error.FormatError.DeserializationError("empty proof");
        }
        Proof proof = new Proof(secretKey, signature);

        return new SerializedBiscuit(authority, blocks, proof);
    }
}
