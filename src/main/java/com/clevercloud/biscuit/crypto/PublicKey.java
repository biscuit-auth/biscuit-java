package com.clevercloud.biscuit.crypto;

import cafe.cryptography.curve25519.CompressedRistretto;
import cafe.cryptography.curve25519.InvalidEncodingException;
import cafe.cryptography.curve25519.RistrettoElement;

public class PublicKey {
    public final RistrettoElement key;


    public PublicKey(RistrettoElement public_key) {
        this.key = public_key;
    }

    public PublicKey(byte[] data) throws InvalidEncodingException {
        CompressedRistretto c = new CompressedRistretto(data);
        this.key = c.decompress();
    }
}
