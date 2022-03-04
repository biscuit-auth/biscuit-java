package com.clevercloud.biscuit.crypto;


import biscuit.format.schema.Schema;
import com.clevercloud.biscuit.token.builder.Utils;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.spec.*;

import java.security.SecureRandom;

/**
 * Private and public key
 */
public final class KeyPair {
    public final EdDSAPrivateKey private_key;
    public final EdDSAPublicKey public_key;

    private static final int ED25519_PUBLIC_KEYSIZE = 32;
    private static final int ED25519_PRIVATE_KEYSIZE = 64;
    private static final int ED25519_SEED_SIZE = 32;
    public static final EdDSANamedCurveSpec ed25519 = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519);

    public KeyPair() {
        this(new SecureRandom());
    }

    public KeyPair(final SecureRandom rng) {
        byte[] b = new byte[32];
        rng.nextBytes(b);

        EdDSAPrivateKeySpec privKeySpec = new EdDSAPrivateKeySpec(b, ed25519);
        EdDSAPrivateKey privKey = new EdDSAPrivateKey(privKeySpec);

        EdDSAPublicKeySpec pubKeySpec = new EdDSAPublicKeySpec(privKey.getA(), ed25519);
        EdDSAPublicKey pubKey = new EdDSAPublicKey(pubKeySpec);

        this.private_key = privKey;
        this.public_key = pubKey;
    }

    public byte[] toBytes() {
        return this.private_key.getSeed();
    }

    public KeyPair(byte[] b) {
        EdDSAPrivateKeySpec privKeySpec = new EdDSAPrivateKeySpec(b, ed25519);
        EdDSAPrivateKey privKey = new EdDSAPrivateKey(privKeySpec);

        EdDSAPublicKeySpec pubKeySpec = new EdDSAPublicKeySpec(privKey.getA(), ed25519);
        EdDSAPublicKey pubKey = new EdDSAPublicKey(pubKeySpec);

        this.private_key = privKey;
        this.public_key = pubKey;
    }

    public String toHex() {
        return Utils.byteArrayToHexString(this.toBytes());
    }

    public KeyPair(String hex) {
        byte[] b = Utils.hexStringToByteArray(hex);

        EdDSAPrivateKeySpec privKeySpec = new EdDSAPrivateKeySpec(b, ed25519);
        EdDSAPrivateKey privKey = new EdDSAPrivateKey(privKeySpec);

        EdDSAPublicKeySpec pubKeySpec = new EdDSAPublicKeySpec(privKey.getA(), ed25519);
        EdDSAPublicKey pubKey = new EdDSAPublicKey(pubKeySpec);

        this.private_key = privKey;
        this.public_key = pubKey;
    }

    public PublicKey public_key() {
        return new PublicKey(Schema.PublicKey.Algorithm.Ed25519, this.public_key);
    }
}
