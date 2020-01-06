package com.clevercloud.biscuit.crypto;

import cafe.cryptography.curve25519.Constants;
import cafe.cryptography.curve25519.RistrettoElement;
import cafe.cryptography.curve25519.Scalar;

import java.security.SecureRandom;

/**
 * Private and public key
 */
public final class KeyPair {
    public final Scalar private_key;
    public final RistrettoElement public_key;

    public KeyPair(final SecureRandom rng) {
        byte[] b = new byte[64];
        rng.nextBytes(b);

        this.private_key = Scalar.fromBytesModOrderWide(b);
        this.public_key = Constants.RISTRETTO_GENERATOR.multiply(this.private_key);
    }

    public byte[] toBytes() {
        return this.private_key.toByteArray();
    }

    public KeyPair(byte[] b) {
        this.private_key = Scalar.fromBytesModOrderWide(b);
        this.public_key = Constants.RISTRETTO_GENERATOR.multiply(this.private_key);
    }

    public String toHex() {
        return byteArrayToHexString(this.toBytes());
    }

    public KeyPair(String hex) {
        byte[] b = hexStringToByteArray(hex);
        this.private_key = Scalar.fromBytesModOrder(b);
        this.public_key = Constants.RISTRETTO_GENERATOR.multiply(this.private_key);
    }

    public PublicKey public_key() {
        return new PublicKey(this.public_key);
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    static String byteArrayToHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    static byte[] hexStringToByteArray(String hex) {
        int l = hex.length();
        byte[] data = new byte[l/2];
        for (int i = 0; i < l; i += 2) {
            data[i/2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }
}
