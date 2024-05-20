package org.biscuitsec.biscuit.crypto;

import org.biscuitsec.biscuit.token.builder.Utils;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

class P256KeyPair extends KeyPair {

    private final java.security.KeyPair keyPair;

    public P256KeyPair() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        this(new SecureRandom());
    }

    public P256KeyPair(byte[] seed) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        var kpg = KeyPairGenerator.getInstance("EC");
        var spec = new ECGenParameterSpec("secp256r1");
        kpg.initialize(spec, new SecureRandom(seed));
        keyPair = kpg.generateKeyPair();
    }

    public P256KeyPair(SecureRandom rng) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        var kpg = KeyPairGenerator.getInstance("EC");
        var spec = new ECGenParameterSpec("secp256r1");
        kpg.initialize(spec, rng);
        keyPair = kpg.generateKeyPair();
    }

    public P256KeyPair(String hex) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        this(Utils.hexStringToByteArray(hex));
    }

    public static java.security.PublicKey generatePublicKey(byte[] data) throws NoSuchAlgorithmException, InvalidKeySpecException {
        return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(data, "SHA256withECDSA"));
    }

    public static Signature getSignature() {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public byte[] toBytes() {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public String toHex() {
        return Utils.byteArrayToHexString(this.toBytes());
    }

    @Override
    public java.security.PublicKey publicKey() {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public PrivateKey private_key() {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public PublicKey public_key() {
        throw new RuntimeException("not yet implemented");
    }
}
