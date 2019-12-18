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

    public byte[] toBytes() {
        return this.key.compress().toByteArray();
    }

    public String toHex() {
        return byteArrayToHexString(this.toBytes());
    }

    public PublicKey(String hex) throws InvalidEncodingException {
        byte[] data = hexStringToByteArray(hex);
        CompressedRistretto c = new CompressedRistretto(data);
        this.key = c.decompress();
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
