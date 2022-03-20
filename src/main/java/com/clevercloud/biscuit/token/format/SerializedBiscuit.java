package com.clevercloud.biscuit.token.format;

import biscuit.format.schema.Schema;
import com.clevercloud.biscuit.crypto.KeyPair;
import com.clevercloud.biscuit.crypto.PublicKey;
import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.token.Block;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.vavr.control.Either;
import io.vavr.control.Option;
import net.i2p.crypto.eddsa.EdDSAEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.*;
import java.util.ArrayList;
import java.util.List;

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

    public static int MAX_SCHEMA_VERSION = 2;

    /**
     * Deserializes a SerializedBiscuit from a byte array
     * @param slice
     * @return
     */
    static public SerializedBiscuit from_bytes(byte[] slice, PublicKey root) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        try {
            //System.out.println("will parse protobuf");
            Schema.Biscuit data = Schema.Biscuit.parseFrom(slice);
            //System.out.println("parse protobuf");

            SignedBlock authority = new SignedBlock(
                    data.getAuthority().getBlock().toByteArray(),
                    new PublicKey(data.getAuthority().getNextKey().getAlgorithm(), data.getAuthority().getNextKey().getKey().toByteArray()),
                    data.getAuthority().getSignature().toByteArray()
            );

            ArrayList<SignedBlock> blocks = new ArrayList<>();
            for (Schema.SignedBlock block : data.getBlocksList()) {
                blocks.add(new SignedBlock(
                        block.getBlock().toByteArray(),
                        new PublicKey(block.getNextKey().getAlgorithm(), block.getNextKey().getKey().toByteArray()),
                        block.getSignature().toByteArray()
                ));
            }

            //System.out.println("parsed blocks");

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

            //System.out.println("parse proof");

            SerializedBiscuit b = new SerializedBiscuit(authority, blocks, proof);

            Either<Error, Void> res = b.verify(root);
            if (res.isLeft()) {
                Error e = res.getLeft();
                //System.out.println("verification error: "+e.toString());
                throw e;
            } else {
                return b;
            }
        } catch (InvalidProtocolBufferException e) {
            throw new Error.FormatError.DeserializationError(e.toString());
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

            SignedBlock authority = new SignedBlock(
                    data.getAuthority().getBlock().toByteArray(),
                    new PublicKey(data.getAuthority().getNextKey().getAlgorithm(), data.getAuthority().getNextKey().getKey().toByteArray()),
                    data.getAuthority().getSignature().toByteArray()
            );

            ArrayList<SignedBlock> blocks = new ArrayList<>();
            for (Schema.SignedBlock block : data.getBlocksList()) {
                blocks.add(new SignedBlock(
                        block.getBlock().toByteArray(),
                        new PublicKey(block.getNextKey().getAlgorithm(), block.getNextKey().getKey().toByteArray()),
                        block.getSignature().toByteArray()
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
        } catch (InvalidProtocolBufferException e) {
            throw new Error.FormatError.DeserializationError(e.toString());
        }
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
            Schema.PublicKey.Builder publicKey = Schema.PublicKey.newBuilder();
            publicKey.setKey(ByteString.copyFrom(block.key.toBytes()));
            publicKey.setAlgorithm(block.key.algorithm);
            authorityBuilder.setBlock(ByteString.copyFrom(block.block));
            authorityBuilder.setNextKey(publicKey.build());
            authorityBuilder.setSignature(ByteString.copyFrom(block.signature));
        }
        biscuitBuilder.setAuthority(authorityBuilder.build());

        for (SignedBlock block : this.blocks) {
            Schema.SignedBlock.Builder blockBuilder = Schema.SignedBlock.newBuilder();
            Schema.PublicKey.Builder publicKey = Schema.PublicKey.newBuilder();
            publicKey.setKey(ByteString.copyFrom(block.key.toBytes()));
            publicKey.setAlgorithm(block.key.algorithm);
            blockBuilder.setBlock(ByteString.copyFrom(block.block));
            blockBuilder.setNextKey(publicKey.build());
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

        //FIXME: set the root key id
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

            SignedBlock signedBlock = new SignedBlock(block, next_key, signature);
            Proof proof = new Proof(next);

            return Right(new SerializedBiscuit(signedBlock, new ArrayList<>(), proof));
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

            SignedBlock signedBlock = new SignedBlock(block, next_key, signature);

            ArrayList<SignedBlock> blocks = new ArrayList<>();
            for (SignedBlock bl : this.blocks) {
                blocks.add(bl);
            }
            blocks.add(signedBlock);

            Proof proof = new Proof(next);

            return Right(new SerializedBiscuit(this.authority, blocks, proof));
        } catch (IOException | NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            return Left(new Error.FormatError.SerializationError(e.toString()));
        }
    }

    public Either<Error, Void> verify(PublicKey root) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        PublicKey current_key = root;
        ByteBuffer algo_buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        {
            byte[] block = this.authority.block;
            PublicKey next_key = this.authority.key;
            byte[] signature = this.authority.signature;
            if(signature.length != 64){
                return Either.left(new Error.FormatError.Signature.InvalidSignatureSize(signature.length));
            }
            algo_buf.putInt(Integer.valueOf(next_key.algorithm.getNumber()));
            algo_buf.flip();

            //System.out.println("verifying block "+"authority"+" with current key "+current_key.toHex()+" block "+block+" next key "+next_key.toHex()+" signature "+signature);

            Signature sgr = new EdDSAEngine(MessageDigest.getInstance(ed25519.getHashAlgorithm()));

            sgr.initVerify(current_key.key);
            sgr.update(block);
            sgr.update(algo_buf);
            sgr.update(next_key.toBytes());
            if (sgr.verify(signature)) {
                current_key = next_key;
            } else {
                return Left(new Error.FormatError.Signature.InvalidSignature("signature error: Verification equation was not satisfied"));
            }
        }

        for (SignedBlock b : this.blocks) {
            byte[] block = b.block;
            PublicKey next_key = b.key;
            byte[] signature = b.signature;
            if(signature.length != 64){
                return Either.left(new Error.FormatError.Signature.InvalidSignatureSize(signature.length));
            }
            algo_buf.clear();
            algo_buf.putInt(Integer.valueOf(next_key.algorithm.getNumber()));
            algo_buf.flip();
            //System.out.println("verifying block ? with current key "+current_key.toHex()+" block "+block+" next key "+next_key.toHex()+" signature "+signature);

            Signature sgr = new EdDSAEngine(MessageDigest.getInstance(ed25519.getHashAlgorithm()));

            sgr.initVerify(current_key.key);
            sgr.update(block);
            sgr.update(algo_buf);
            sgr.update(next_key.toBytes());
            if (sgr.verify(signature)) {
                current_key = next_key;
            } else {
                return Left(new Error.FormatError.Signature.InvalidSignature("signature error: Verification equation was not satisfied"));
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
    }
}
