package org.biscuitsec.biscuit.crypto;


import biscuit.format.schema.Schema.PublicKey.Algorithm;
import net.i2p.crypto.eddsa.Utils;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Signature;

/**
 * Private and public key.
 */
public abstract class KeyPair {

    public static KeyPair generate(Algorithm algorithm, String hex) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        return generate(algorithm, Utils.hexToBytes(hex));
    }

    public static KeyPair generate(Algorithm algorithm, byte[] bytes) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        if (algorithm == Algorithm.Ed25519) {
            return new Ed25519KeyPair(bytes);
        } else if (algorithm == Algorithm.P256) {
            return new P256KeyPair(bytes);
        } else {
            throw new NoSuchAlgorithmException("Unsupported algorithm");
        }
    }

    public static KeyPair generate(Algorithm algorithm, SecureRandom rng) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        if (algorithm == Algorithm.Ed25519) {
            return new Ed25519KeyPair(rng);
        } else if (algorithm == Algorithm.P256) {
            return new P256KeyPair(rng);
        } else {
            throw new NoSuchAlgorithmException("Unsupported algorithm");
        }
    }

    public static Signature generateSignature(Algorithm algorithm) throws NoSuchAlgorithmException {
        if (algorithm == Algorithm.Ed25519) {
            return Ed25519KeyPair.getSignature();
        } else if (algorithm == Algorithm.P256) {
            return P256KeyPair.getSignature();
        } else {
            throw new NoSuchAlgorithmException("Unsupported algorithm");
        }
    }

    /**
     * Returns the seed bytes of the private key. Returns null if the key was not created from a seed.
     * @return seed bytes.
     */
    public abstract byte[] toBytes(); // todo: rename to getSeedBytes()

    /**
     * Returns the hex representation of the seed bytes of the private key. Null if the key was not created from a seed.
     * @return hex representation of the seed bytes.
     */
    public abstract String toHex(); // todo: rename to getHex()

    /**
     * Returns the java.security.PublicKey.
     * @return
     */
    public abstract java.security.PublicKey publicKey();

    /**
     * Returns the java.security.PrivateKey.
     * @return
     */
    public abstract java.security.PrivateKey private_key();

    /**
     * Returns the biscuit.crypto.PublicKey.
     * @return
     */
    public abstract PublicKey public_key();
}
