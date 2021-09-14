package com.clevercloud.biscuit.crypto;

import com.clevercloud.biscuit.token.builder.Utils;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

import static com.clevercloud.biscuit.crypto.KeyPair.ed25519;

public class PublicKey {
    public final EdDSAPublicKey key;


    public PublicKey(EdDSAPublicKey public_key) {
        this.key = public_key;
    }

    public PublicKey(byte[] data) {
        EdDSAPublicKeySpec pubKeySpec = new EdDSAPublicKeySpec(data, ed25519);
        EdDSAPublicKey pubKey = new EdDSAPublicKey(pubKeySpec);
        this.key = pubKey;
    }

    public byte[] toBytes() {
        return this.key.getAbyte();
    }

    public String toHex() {
        return Utils.byteArrayToHexString(this.toBytes());
    }

    public PublicKey(String hex) {
        byte[] data = Utils.hexStringToByteArray(hex);
        EdDSAPublicKeySpec pubKeySpec = new EdDSAPublicKeySpec(data, ed25519);
        EdDSAPublicKey pubKey = new EdDSAPublicKey(pubKeySpec);
        this.key = pubKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PublicKey publicKey = (PublicKey) o;

        return key.equals(publicKey.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
}
