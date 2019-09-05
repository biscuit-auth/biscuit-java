package com.clevercloud.biscuit.crypto;

import cafe.cryptography.curve25519.RistrettoElement;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class Token {
    public final ArrayList<byte[]> messages; //= new ArrayList<>();
    public final ArrayList<RistrettoElement> keys; // keys = new ArrayList<>();
    public final TokenSignature signature;

    public Token(final SecureRandom rng, KeyPair keypair, byte[] message) {
        this.signature = new TokenSignature(rng, keypair, message);
        this.messages = new ArrayList<>();
        this.messages.add(message);
        this.keys = new ArrayList<>();
        this.keys.add(keypair.public_key);
    }

    public Token(final ArrayList<byte[]> messages, final ArrayList<RistrettoElement> keys, final TokenSignature signature) {
        this.signature = signature;
        this.messages = messages;
        this.keys = keys;
    }

    public Token append(final SecureRandom rng, KeyPair keypair, byte[] message) {
        TokenSignature signature = this.signature.sign(rng, this.keys, this.messages, keypair, message);


        Token token = new Token(this.messages, this.keys, signature);
        token.messages.add(message);
        token.keys.add(keypair.public_key);

        return token;
    }

    // FIXME: rust version returns a Result<(), error::Signature>
    public boolean verify() {
        return this.signature.verify(this.keys, this.messages);
    }
}
