package com.clevercloud.biscuit.token.format;

import biscuit.format.schema.Schema;
import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.token.Block;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.vavr.control.Either;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

public class SealedBiscuit {
    public byte[] authority;
    public List<byte[]> blocks;
    public byte[] signature;

    /**
     * Deserializes a SealedBiscuit from a byte array
     * @param slice
     * @return
     */
    static public Either<Error, SealedBiscuit> from_bytes(byte[] slice, byte[] secret) {
        try {
            Schema.SealedBiscuit data = Schema.SealedBiscuit.parseFrom(slice);

            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret, "HmacSHA256");
            sha256_HMAC.init(secret_key);

            byte[] authority = data.getAuthority().toByteArray();
            sha256_HMAC.update(authority);

            ArrayList<byte[]> blocks = new ArrayList<>();
            for (ByteString block: data.getBlocksList()) {
                byte[] b = block.toByteArray();
                blocks.add(b);
                sha256_HMAC.update(b);
            }

            byte[] calculated = sha256_HMAC.doFinal();
            byte[] signature = data.getSignature().toByteArray();

            if (calculated.length != signature.length) {
                return Left(new Error.FormatError.Signature.InvalidFormat());
            }

            int result = 0;
            for (int i = 0; i < calculated.length; i++) {
                result |= calculated[i] ^ signature[i];
            }

            if (result != 0) {
                return Left(new Error.FormatError.SealedSignature());
            }

            SealedBiscuit b = new SealedBiscuit(authority, blocks, signature);
            return Right(b);
        } catch(InvalidProtocolBufferException | NoSuchAlgorithmException | InvalidKeyException e) {
            return Left(new Error.FormatError.DeserializationError(e.toString()));
        }
    }

    /**
     * Serializes a SealedBiscuit to a byte array
     * @return
     */
    public Either<Error.FormatError, byte[]> serialize() {
        Schema.SealedBiscuit.Builder b = Schema.SealedBiscuit.newBuilder()
                .setSignature(ByteString.copyFrom(this.signature));

        b.setAuthority(ByteString.copyFrom(this.authority));

        for (int i = 0; i < this.blocks.size(); i++) {
            b.addBlocks(ByteString.copyFrom(this.blocks.get(i)));
        }

        Schema.SealedBiscuit biscuit = b.build();

        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            biscuit.writeTo(stream);
            byte[] data = stream.toByteArray();
            return Right(data);
        } catch(IOException e) {
            return Left(new Error.FormatError.SerializationError(e.toString()));
        }

    }

    SealedBiscuit(byte[] authority, List<byte[]> blocks, byte[] signature) {
        this.authority = authority;
        this.blocks = blocks;
        this.signature = signature;
    }

    public static Either<Error.FormatError, SealedBiscuit> make(Block authority, List<Block> blocks, byte[] secret) {

        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret, "HmacSHA256");
            sha256_HMAC.init(secret_key);

            Schema.Block b = authority.serialize();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            b.writeTo(stream);
            byte[] authorityData = stream.toByteArray();
            sha256_HMAC.update(authorityData);


            ArrayList<byte[]> blocksData = new ArrayList<>();
            for(Block bl: blocks) {
                Schema.Block b2 = bl.serialize();
                ByteArrayOutputStream stream2 = new ByteArrayOutputStream();
                b2.writeTo(stream2);
                sha256_HMAC.update(stream2.toByteArray());
                blocksData.add(stream2.toByteArray());
            }

            byte[] signature = sha256_HMAC.doFinal();
            return Right(new SealedBiscuit(authorityData, blocksData, signature));
        } catch(IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            return Left(new Error.FormatError.SerializationError(e.toString()));
        }
    }

    public List<byte[]> revocation_identifiers() {
        ArrayList<byte[]> l = new ArrayList<>();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(this.authority);
            MessageDigest cloned = (MessageDigest)digest.clone();
            l.add(digest.digest());

            digest = cloned;

            for(int i = 0; i < this.blocks.size(); i++) {
                byte[] block = this.blocks.get(i);
                digest.update(block);
                cloned = (MessageDigest)digest.clone();
                l.add(digest.digest());

                digest = cloned;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return l;
    }
}
