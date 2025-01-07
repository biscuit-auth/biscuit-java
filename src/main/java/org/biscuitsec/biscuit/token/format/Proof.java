package org.biscuitsec.biscuit.token.format;

import io.vavr.control.Option;
import org.biscuitsec.biscuit.crypto.KeyPair;

public class Proof {
    public Option<KeyPair> secretKey;
    public Option<byte[]> signature;

    public Proof(Option<KeyPair> secretKey, Option<byte[]> signature) {
        this.secretKey = secretKey;
        this.signature = signature;
    }

    public Proof(KeyPair secretKey) {
        this.secretKey = Option.some(secretKey);
        this.signature = Option.none();
    }

    public Proof(byte[] signature) {
        this.secretKey = Option.none();
        this.signature = Option.some(signature);
    }
}
