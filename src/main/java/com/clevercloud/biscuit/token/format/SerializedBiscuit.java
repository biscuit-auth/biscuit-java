package com.clevercloud.biscuit.token.format;

import biscuit.format.schema.Schema;
import cafe.cryptography.curve25519.CompressedRistretto;
import cafe.cryptography.curve25519.InvalidEncodingException;
import cafe.cryptography.curve25519.RistrettoElement;
import com.clevercloud.biscuit.crypto.KeyPair;
import com.clevercloud.biscuit.crypto.PublicKey;
import com.clevercloud.biscuit.crypto.TokenSignature;
import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.token.Block;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.vavr.control.Either;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

/**
 * Intermediate representation of a token before full serialization
 */
public class SerializedBiscuit {
    public byte[] authority;
    public List<byte[]> blocks;
    public List<RistrettoElement> keys;
    public TokenSignature signature;

    /**
     * Deserializes a SerializedBiscuit from a byte array
     * @param slice
     * @return
     */
    static public Either<Error, SerializedBiscuit> from_bytes(byte[] slice) {
        try {
            Schema.Biscuit data = Schema.Biscuit.parseFrom(slice);

            ArrayList<RistrettoElement> keys = new ArrayList<>();
            for (ByteString key: data.getKeysList()) {
                keys.add((new CompressedRistretto(key.toByteArray())).decompress());
            }

            byte[] authority = data.getAuthority().toByteArray();

            ArrayList<byte[]> blocks = new ArrayList<>();
            for (ByteString block: data.getBlocksList()) {
                blocks.add(block.toByteArray());
            }

            Either<Error, TokenSignature> signatureRes = TokenSignature.deserialize(data.getSignature());

            if(signatureRes.isLeft()) {
                Error e = signatureRes.getLeft();
                return Left(e);
            }

            TokenSignature signature = signatureRes.get();

            SerializedBiscuit b = new SerializedBiscuit(authority, blocks, keys, signature);

            Either<Error, Void> res = b.verify();
            if(res.isLeft()) {
                Error e = res.getLeft();
                return Left(e);
            } else {
                return Right(b);
            }
        } catch(InvalidProtocolBufferException e) {
            return Left(new Error().new FormatError().new DeserializationError(e.toString()));
        } catch(InvalidEncodingException e) {
            return Left(new Error().new FormatError().new DeserializationError(e.toString()));
        }
    }

    /**
     * Serializes a SerializedBiscuit to a byte array
     * @return
     */
    public Either<Error, byte[]> serialize() {
        Schema.Biscuit.Builder b = Schema.Biscuit.newBuilder()
                .setSignature(this.signature.serialize());

        for (int i = 0; i < this.keys.size(); i++) {
            b.addKeys(ByteString.copyFrom(this.keys.get(i).compress().toByteArray()));
        }

        b.setAuthority(ByteString.copyFrom(this.authority));

        for (int i = 0; i < this.blocks.size(); i++) {
            b.addBlocks(ByteString.copyFrom(this.blocks.get(i)));
        }

        Schema.Biscuit biscuit = b.build();

        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            biscuit.writeTo(stream);
            byte[] data = stream.toByteArray();
            return Right(data);
        } catch(IOException e) {
            return Left(new Error().new FormatError().new SerializationError(e.toString()));
        }

    }

    static public Either<Error.FormatError, SerializedBiscuit> make(final SecureRandom rng, final KeyPair root,
                                                             final Block authority) {
        Schema.Block b = authority.serialize();
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            b.writeTo(stream);
            byte[] data = stream.toByteArray();

            TokenSignature signature = new TokenSignature(rng, root, data);
            ArrayList<RistrettoElement> keys = new ArrayList<>();
            keys.add(root.public_key);

            return Right(new SerializedBiscuit(data, new ArrayList<>(), keys, signature));
        } catch(IOException e) {
            return Left(new Error().new FormatError().new SerializationError(e.toString()));
        }
    }

    public Either<Error.FormatError, SerializedBiscuit> append(final SecureRandom rng, final KeyPair keypair,
                                                               final Block block) {
        Schema.Block b = block.serialize();
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            b.writeTo(stream);
            byte[] data = stream.toByteArray();

            TokenSignature signature = this.signature.sign(rng, keypair, data);

            ArrayList<RistrettoElement> keys = new ArrayList<>();
            for(RistrettoElement key: this.keys) {
                keys.add(key);
            }
            keys.add(keypair.public_key);

            ArrayList<byte[]> blocks = new ArrayList<>();
            for(byte[] bl: this.blocks) {
                blocks.add(bl);
            }
            blocks.add(data);

            return Right(new SerializedBiscuit(this.authority, blocks, keys, signature));
        } catch(IOException e) {
            return Left(new Error().new FormatError().new SerializationError(e.toString()));
        }
    }

    public Either<Error, Void> verify() {
        if(this.keys.isEmpty()) {
            return Left(new Error().new FormatError().new EmptyKeys());
        }

        ArrayList<byte[]> blocks = new ArrayList<>();
        blocks.add(this.authority);
        for(byte[] bl: this.blocks) {
            blocks.add(bl);
        }

        return this.signature.verify(this.keys, blocks);
    }

    public Either<Error, Void> check_root_key(PublicKey public_key) {
        if(this.keys.isEmpty()) {
            return Left(new Error().new FormatError().new EmptyKeys());
        }

        if(!(this.keys.get(0).ctEquals(public_key.key) == 1)) {
            return Left(new Error().new FormatError().new UnknownPublicKey());
        }

        return Right(null);
    }


    SerializedBiscuit(byte[] authority, List<byte[]> blocks, List<RistrettoElement> keys, TokenSignature signature) {
        this.authority = authority;
        this.blocks = blocks;
        this.keys = keys;
        this.signature = signature;
    }
}
