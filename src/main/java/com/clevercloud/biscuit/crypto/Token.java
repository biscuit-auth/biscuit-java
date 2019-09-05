package com.clevercloud.biscuit.crypto;

import biscuit.format.schema.Schema;
import cafe.cryptography.curve25519.CompressedRistretto;
import cafe.cryptography.curve25519.InvalidEncodingException;
import cafe.cryptography.curve25519.RistrettoElement;
import cafe.cryptography.curve25519.Scalar;
import com.google.protobuf.ByteString;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class Token {
    public final ArrayList<byte[]> blocks; //= new ArrayList<>();
    public final ArrayList<RistrettoElement> keys; // keys = new ArrayList<>();
    public final TokenSignature signature;

    public Token(final SecureRandom rng, KeyPair keypair, byte[] message) {
        this.signature = new TokenSignature(rng, keypair, message);
        this.blocks = new ArrayList<>();
        this.blocks.add(message);
        this.keys = new ArrayList<>();
        this.keys.add(keypair.public_key);
    }

    public Token(final ArrayList<byte[]> blocks, final ArrayList<RistrettoElement> keys, final TokenSignature signature) {
        this.signature = signature;
        this.blocks = blocks;
        this.keys = keys;
    }

    public Token append(final SecureRandom rng, KeyPair keypair, byte[] message) {
        TokenSignature signature = this.signature.sign(rng, this.keys, this.blocks, keypair, message);


        Token token = new Token(this.blocks, this.keys, signature);
        token.blocks.add(message);
        token.keys.add(keypair.public_key);

        return token;
    }

    // FIXME: rust version returns a Result<(), error::Signature>
    public boolean verify() {
        return this.signature.verify(this.keys, this.blocks);
    }

    public Schema.Biscuit serialize() {
        Schema.Biscuit.Builder token = Schema.Biscuit.newBuilder()
                .setSignature(this.signature.serialize());

        for (int i = 0; i < this.keys.size(); i++) {
            token.addKeys(ByteString.copyFrom(this.keys.get(i).compress().toByteArray()));
        }

        //FIXME: assert at least one element
        token.setAuthority(ByteString.copyFrom(this.blocks.get(0)));

        for (int i = 1; i < this.keys.size(); i++) {
            token.addBlocks(ByteString.copyFrom(this.blocks.get(i)));
        }

        return token.build();
    }

    static public Token deserialize(Schema.Biscuit token) throws InvalidEncodingException {
        TokenSignature signature = TokenSignature.deserialize(token.getSignature());

        ArrayList<RistrettoElement> keys = new ArrayList<>();
        for (ByteString key: token.getKeysList()) {
            keys.add((new CompressedRistretto(key.toByteArray())).decompress());
        }

        ArrayList<byte[]> blocks = new ArrayList<>();
        blocks.add(token.getAuthority().toByteArray());
        for (ByteString block: token.getBlocksList()) {
            blocks.add(block.toByteArray());
        }

        return new Token(blocks, keys, signature);
    }
}
