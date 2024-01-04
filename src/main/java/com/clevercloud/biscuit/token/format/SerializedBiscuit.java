package com.clevercloud.biscuit.token.format;

import biscuit.format.schema.Schema;
import com.clevercloud.biscuit.crypto.KeyDelegate;
import com.clevercloud.biscuit.crypto.KeyPair;
import com.clevercloud.biscuit.crypto.PublicKey;
import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.token.Block;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.vavr.Tuple3;
import io.vavr.control.Either;
import io.vavr.control.Option;
import net.i2p.crypto.eddsa.EdDSAEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.*;
import java.util.*;

import static com.clevercloud.biscuit.crypto.KeyPair.ed25519;
import static io.vavr.API.Left;
import static io.vavr.API.Right;

/**
 * Intermediate representation of a token before full serialization
 */
public class SerializedBiscuit {
    public SignedBlock authority;
    public List<SignedBlock> blocks;
    public Proof proof;
    public Option<Integer> root_key_id;

    public static int MIN_SCHEMA_VERSION = 3;
    public static int MAX_SCHEMA_VERSION = 4;

    /**
     * Deserializes a SerializedBiscuit from a byte array
     *
     * @param slice
     * @return
     */
    static public SerializedBiscuit from_bytes(byte[] slice, PublicKey root) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        try {
            Schema.Biscuit data = Schema.Biscuit.parseFrom(slice);

            return from_bytes_inner(data, root);
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
    static public SerializedBiscuit from_bytes(byte[] slice, KeyDelegate delegate) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        try {
            Schema.Biscuit data = Schema.Biscuit.parseFrom(slice);

            Option<Integer> root_key_id = Option.none();
            if (data.hasRootKeyId()) {
                root_key_id = Option.some(data.getRootKeyId());
            }

            Option<PublicKey> root = delegate.root_key(root_key_id);
            if (root.isEmpty()) {
                throw new InvalidKeyException("unknown root key id");
            }

            return from_bytes_inner(data, root.get());
        } catch (InvalidProtocolBufferException e) {
            throw new Error.FormatError.DeserializationError(e.toString());
        }
    }

    static SerializedBiscuit from_bytes_inner(Schema.Biscuit data, PublicKey root) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        SerializedBiscuit b = SerializedBiscuit.deserialize(data);
        if (data.hasRootKeyId()) {
            b.root_key_id = Option.some(data.getRootKeyId());
        }

        Either<Error, Void> res = b.verify(root);
        if (res.isLeft()) {
            Error e = res.getLeft();
            //System.out.println("verification error: "+e.toString());
            throw e;
        } else {
            return b;
        }

    }

    /**
     * Warning: this deserializes without verifying the signature
     *
     * @param slice
     * @return SerializedBiscuit
     * @throws Error.FormatError.DeserializationError
     */
    static public SerializedBiscuit unsafe_deserialize(byte[] slice) throws Error.FormatError.DeserializationError {
        try {
            Schema.Biscuit data = Schema.Biscuit.parseFrom(slice);
            return SerializedBiscuit.deserialize(data);
        } catch (InvalidProtocolBufferException e) {
            throw new Error.FormatError.DeserializationError(e.toString());
        }
    }

    /**
     * Warning: this deserializes without verifying the signature
     *
     * @param data
     * @return SerializedBiscuit
     * @throws Error.FormatError.DeserializationError
     */
    static private SerializedBiscuit deserialize(Schema.Biscuit data) throws Error.FormatError.DeserializationError {
        if(data.getAuthority().hasExternalSignature()) {
            throw new Error.FormatError.DeserializationError("the authority block must not contain an external signature");
        }

        SignedBlock authority = new SignedBlock(
                data.getAuthority().getBlock().toByteArray(),
                PublicKey.deserialize(data.getAuthority().getNextKey()),
                data.getAuthority().getSignature().toByteArray(),
                Option.none()
        );

        ArrayList<SignedBlock> blocks = new ArrayList<>();
        for (Schema.SignedBlock block : data.getBlocksList()) {
            Option<ExternalSignature> external = Option.none();
            if(block.hasExternalSignature() && block.getExternalSignature().hasPublicKey()
                && block.getExternalSignature().hasSignature()) {
                Schema.ExternalSignature ex = block.getExternalSignature();
                external = Option.some(new ExternalSignature(
                        PublicKey.deserialize(ex.getPublicKey()),
                        ex.getSignature().toByteArray()));

            }
            blocks.add(new SignedBlock(
                    block.getBlock().toByteArray(),
                    PublicKey.deserialize(block.getNextKey()),
                    block.getSignature().toByteArray(),
                    external
            ));
        }

        Option<KeyPair> secretKey = Option.none();
        if (data.getProof().hasNextSecret()) {
            secretKey = Option.some(new KeyPair(data.getProof().getNextSecret().toByteArray()));
        }

        Option<byte[]> signature = Option.none();
        if (data.getProof().hasFinalSignature()) {
            signature = Option.some(data.getProof().getFinalSignature().toByteArray());
        }

        if (secretKey.isEmpty() && signature.isEmpty()) {
            throw new Error.FormatError.DeserializationError("empty proof");
        }
        Proof proof = new Proof(secretKey, signature);

        SerializedBiscuit b = new SerializedBiscuit(authority, blocks, proof);
        return b;
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

            biscuitBuilder.addBlocks(blockBuilder.build());
        }

        Schema.Proof.Builder proofBuilder = Schema.Proof.newBuilder();
        if (!this.proof.secretKey.isEmpty()) {
            proofBuilder.setNextSecret(ByteString.copyFrom(this.proof.secretKey.get().toBytes()));
        } else {
            proofBuilder.setFinalSignature(ByteString.copyFrom(this.proof.signature.get()));
        }

        biscuitBuilder.setProof(proofBuilder.build());
        if (!this.root_key_id.isEmpty()) {
            biscuitBuilder.setRootKeyId(this.root_key_id.get());
        }

        Schema.Biscuit biscuit = biscuitBuilder.build();

        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            biscuit.writeTo(stream);
            byte[] data = stream.toByteArray();
            return data;
        } catch (IOException e) {
            throw new Error.FormatError.SerializationError(e.toString());
        }

    }

    static public Either<Error.FormatError, SerializedBiscuit> make(final KeyPair root,
                                                                    final Block authority, final KeyPair next) {

        return make(root, Option.none(), authority, next);
    }

    static public Either<Error.FormatError, SerializedBiscuit> make(final KeyPair root, final Option<Integer> root_key_id,
                                                                    final Block authority, final KeyPair next) {
        Schema.Block b = authority.serialize();
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            b.writeTo(stream);
            byte[] block = stream.toByteArray();
            PublicKey next_key = next.public_key();
            ByteBuffer algo_buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
            algo_buf.putInt(Integer.valueOf(next_key.algorithm.getNumber()));
            algo_buf.flip();

            Signature sgr = new EdDSAEngine(MessageDigest.getInstance(ed25519.getHashAlgorithm()));
            sgr.initSign(root.private_key);
            sgr.update(block);
            sgr.update(algo_buf);
            sgr.update(next_key.toBytes());
            byte[] signature = sgr.sign();

            SignedBlock signedBlock = new SignedBlock(block, next_key, signature, Option.none());
            Proof proof = new Proof(next);

            return Right(new SerializedBiscuit(signedBlock, new ArrayList<>(), proof, root_key_id));
        } catch (IOException | NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            return Left(new Error.FormatError.SerializationError(e.toString()));
        }
    }

    public Either<Error.FormatError, SerializedBiscuit> append(final KeyPair next,
                                                               final Block newBlock) {
        if (this.proof.secretKey.isEmpty()) {
            return Left(new Error.FormatError.SerializationError("the token is sealed"));
        }

        Schema.Block b = newBlock.serialize();
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            b.writeTo(stream);

            byte[] block = stream.toByteArray();
            PublicKey next_key = next.public_key();
            ByteBuffer algo_buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
            algo_buf.putInt(Integer.valueOf(next_key.algorithm.getNumber()));
            algo_buf.flip();

            Signature sgr = new EdDSAEngine(MessageDigest.getInstance(ed25519.getHashAlgorithm()));
            sgr.initSign(this.proof.secretKey.get().private_key);
            sgr.update(block);
            sgr.update(algo_buf);
            sgr.update(next_key.toBytes());
            byte[] signature = sgr.sign();

            SignedBlock signedBlock = new SignedBlock(block, next_key, signature, Option.none());

            ArrayList<SignedBlock> blocks = new ArrayList<>();
            for (SignedBlock bl : this.blocks) {
                blocks.add(bl);
            }
            blocks.add(signedBlock);

            Proof proof = new Proof(next);

            return Right(new SerializedBiscuit(this.authority, blocks, proof, root_key_id));
        } catch (IOException | NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            return Left(new Error.FormatError.SerializationError(e.toString()));
        }
    }

    public Either<Error.FormatError, SerializedBiscuit> appendThirdParty(final KeyPair next,
                                                               final Block newBlock) {
        /*if (this.proof.secretKey.isEmpty()) {
            return Left(new Error.FormatError.SerializationError("the token is sealed"));
        }

        Schema.Block b = newBlock.serialize();
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            b.writeTo(stream);

            byte[] block = stream.toByteArray();
            PublicKey next_key = next.public_key();
            ByteBuffer algo_buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
            algo_buf.putInt(Integer.valueOf(next_key.algorithm.getNumber()));
            algo_buf.flip();

            Signature sgr = new EdDSAEngine(MessageDigest.getInstance(ed25519.getHashAlgorithm()));
            sgr.initSign(this.proof.secretKey.get().private_key);
            sgr.update(block);
            sgr.update(algo_buf);
            sgr.update(next_key.toBytes());
            byte[] signature = sgr.sign();

            SignedBlock signedBlock = new SignedBlock(block, next_key, signature);

            ArrayList<SignedBlock> blocks = new ArrayList<>();
            for (SignedBlock bl : this.blocks) {
                blocks.add(bl);
            }
            blocks.add(signedBlock);

            Proof proof = new Proof(next);

            return Right(new SerializedBiscuit(this.authority, blocks, proof, root_key_id));
        } catch (IOException | NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            return Left(new Error.FormatError.SerializationError(e.toString()));
        }*/
        throw new RuntimeException("todo");
    }

    public Either<Error, Void> verify(PublicKey root) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        PublicKey current_key = root;
        ByteBuffer algo_buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        {
            Either<Error, PublicKey> res = verifyBlockSignature(this.authority, current_key);
            if(res.isRight()) {
                current_key = res.get();
            } else {
                return Left(res.getLeft());
            }
        }

        for (SignedBlock b : this.blocks) {
            Either<Error, PublicKey> res = verifyBlockSignature(b, current_key);
            if(res.isRight()) {
                current_key = res.get();
            } else {
                return Left(res.getLeft());
            }
        }

        //System.out.println("signatures verified, checking proof");

        if (!this.proof.secretKey.isEmpty()) {
            //System.out.println("checking secret key");
            //System.out.println("current key: "+current_key.toHex());
            //System.out.println("key from proof: "+this.proof.secretKey.get().public_key().toHex());
            if (this.proof.secretKey.get().public_key().equals(current_key)) {
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
            PublicKey next_key = b.key;
            byte[] signature = b.signature;
            algo_buf.clear();
            algo_buf.putInt(next_key.algorithm.getNumber());
            algo_buf.flip();

            Signature sgr = new EdDSAEngine(MessageDigest.getInstance(ed25519.getHashAlgorithm()));

            sgr.initVerify(current_key.key);
            sgr.update(block);
            sgr.update(algo_buf);
            sgr.update(next_key.toBytes());
            sgr.update(signature);

            if (sgr.verify(finalSignature)) {
                return Right(null);
            } else {
                return Left(new Error.FormatError.Signature.SealedSignature());
            }

        }
    }

    static Either<Error, PublicKey> verifyBlockSignature(SignedBlock signedBlock, PublicKey publicKey)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        byte[] block = signedBlock.block;
        PublicKey next_key = signedBlock.key;
        byte[] signature = signedBlock.signature;
        if (signature.length != 64) {
            return Either.left(new Error.FormatError.Signature.InvalidSignatureSize(signature.length));
        }
        ByteBuffer algo_buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        algo_buf.putInt(Integer.valueOf(next_key.algorithm.getNumber()));
        algo_buf.flip();

        Signature sgr = new EdDSAEngine(MessageDigest.getInstance(ed25519.getHashAlgorithm()));

        sgr.initVerify(publicKey.key);
        sgr.update(block);
        if(signedBlock.externalSignature.isDefined()) {
            sgr.update(signedBlock.externalSignature.get().signature);
        }
        sgr.update(algo_buf);
        sgr.update(next_key.toBytes());
        if (!sgr.verify(signature)) {
            return Left(new Error.FormatError.Signature.InvalidSignature("signature error: Verification equation was not satisfied"));
        }

        if(signedBlock.externalSignature.isDefined()) {
            ByteBuffer algo_buf2 = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
            algo_buf2.putInt(Integer.valueOf(publicKey.algorithm.getNumber()));
            algo_buf2.flip();

            Signature sgr2 = new EdDSAEngine(MessageDigest.getInstance(ed25519.getHashAlgorithm()));
            sgr2.initVerify(signedBlock.externalSignature.get().key.key);
            sgr2.update(block);
            sgr2.update(algo_buf2);
            sgr2.update(publicKey.toBytes());
            if (!sgr2.verify(signedBlock.externalSignature.get().signature)) {
                return Left(new Error.FormatError.Signature.InvalidSignature("external signature error: Verification equation was not satisfied"));
            }
        }

        return Right(next_key);
    }

    public Tuple3<Block, ArrayList<Block>, HashMap<Long, List<Long>>> extractBlocks(SymbolTable symbols) throws Error {
        ArrayList<Option<PublicKey>> blockExternalKeys = new ArrayList<>();
        Either<Error.FormatError, Block> authRes = Block.from_bytes(this.authority.block);
        if (authRes.isLeft()) {
            Error e = authRes.getLeft();
            throw e;
        }
        Block authority = authRes.get();
        for(PublicKey pk: authority.publicKeys()) {
            symbols.publicKeys.add(pk);
        }
        blockExternalKeys.add(Option.none());


        for (String s : authority.symbols().symbols) {
            symbols.add(s);
        }


        ArrayList<Block> blocks = new ArrayList<>();
        for (SignedBlock bdata : this.blocks) {
            Either<Error.FormatError, Block> blockRes = Block.from_bytes(bdata.block);
            if (blockRes.isLeft()) {
                Error e = blockRes.getLeft();
                throw e;
            }
            Block block = blockRes.get();

            // blocks with external signatures keep their own symbol table
            if(bdata.externalSignature.isDefined()) {
                symbols.publicKeys.add(bdata.externalSignature.get().key);
                blockExternalKeys.add(Option.some(bdata.externalSignature.get().key));
            } else {
                blockExternalKeys.add(Option.none());
                for (String s : block.symbols().symbols) {
                    symbols.add(s);
                }
            }
            for(PublicKey pk: block.publicKeys()) {
                symbols.publicKeys.add(pk);
            }
            blocks.add(block);
        }

        HashMap<Long, List<Long>> publicKeyToBlockId = new HashMap<>();
        for(int blockIndex = 0; blockIndex < blockExternalKeys.size(); blockIndex++) {
            if(blockExternalKeys.get(blockIndex).isDefined()) {
                PublicKey pk = blockExternalKeys.get(blockIndex).get();
                long keyIndex = symbols.insert(pk);
                if(!publicKeyToBlockId.containsKey(keyIndex)) {
                    publicKeyToBlockId.put(keyIndex, new ArrayList<>());
                }
                publicKeyToBlockId.get(keyIndex).add((long) blockIndex);
            }
        }
        return new Tuple3<>(authority, blocks, publicKeyToBlockId);
    }

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
        ByteBuffer algo_buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        algo_buf.putInt(Integer.valueOf(block.key.algorithm.getNumber()));
        algo_buf.flip();


        sgr.initSign(this.proof.secretKey.get().private_key);
        sgr.update(block.block);
        sgr.update(algo_buf);
        sgr.update(block.key.toBytes());
        sgr.update(block.signature);

        byte[] signature = sgr.sign();

        this.proof.secretKey = Option.none();
        this.proof.signature = Option.some(signature);

        return Right(null);
    }

    public List<byte[]> revocation_identifiers() {
        ArrayList<byte[]> l = new ArrayList<>();
        l.add(this.authority.signature);

        for (SignedBlock block : this.blocks) {
            l.add(block.signature);
        }
        return l;
    }

    SerializedBiscuit(SignedBlock authority, List<SignedBlock> blocks, Proof proof) {
        this.authority = authority;
        this.blocks = blocks;
        this.proof = proof;
        this.root_key_id = Option.none();
    }

    SerializedBiscuit(SignedBlock authority, List<SignedBlock> blocks, Proof proof, Option<Integer> root_key_id) {
        this.authority = authority;
        this.blocks = blocks;
        this.proof = proof;
        this.root_key_id = root_key_id;
    }
}
