package com.clevercloud.biscuit.crypto;

import cafe.cryptography.curve25519.CompressedRistretto;
import cafe.cryptography.curve25519.InvalidEncodingException;
import cafe.cryptography.curve25519.RistrettoElement;
import com.clevercloud.biscuit.token.builder.Utils;

public class PublicKey {
    public final RistrettoElement key;


    public PublicKey(RistrettoElement public_key) {
        this.key = public_key;
    }

    public PublicKey(byte[] data) throws InvalidEncodingException {
        CompressedRistretto c = new CompressedRistretto(data);
        this.key = c.decompress();
    }

    public byte[] toBytes() {
        return this.key.compress().toByteArray();
    }

    public String toHex() {
        return Utils.byteArrayToHexString(this.toBytes());
    }

    public PublicKey(String hex) throws InvalidEncodingException {
        byte[] data = Utils.hexStringToByteArray(hex);
        CompressedRistretto c = new CompressedRistretto(data);
        this.key = c.decompress();
    }
}
