package org.biscuitsec.biscuit.crypto;


import biscuit.format.schema.Schema;
import biscuit.format.schema.Schema.PublicKey.Algorithm;
import net.i2p.crypto.eddsa.EdDSAEngine;
import org.biscuitsec.biscuit.token.builder.Utils;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.spec.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Signature;

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

    public static KeyPair generate(Algorithm algorithm) {
        return generate(algorithm, new SecureRandom());
    }

    public static KeyPair generate(Algorithm algorithm, SecureRandom rng) {
        if (algorithm == Algorithm.Ed25519) {
            return new KeyPair(rng);
        } else {
            throw new IllegalArgumentException("Unsupported algorithm");
        }
    }

    public static Signature generateSignature(Algorithm algorithm) throws NoSuchAlgorithmException {
        if (algorithm == Algorithm.Ed25519) {
            return KeyPair.getSignature();
        } else {
            throw new NoSuchAlgorithmException("Unsupported algorithm");
        }
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

    public static Signature getSignature() throws NoSuchAlgorithmException {
        return new EdDSAEngine(MessageDigest.getInstance(ed25519.getHashAlgorithm()));
    }

    public PublicKey public_key() {
        return new PublicKey(Schema.PublicKey.Algorithm.Ed25519, this.public_key);
    }
}
