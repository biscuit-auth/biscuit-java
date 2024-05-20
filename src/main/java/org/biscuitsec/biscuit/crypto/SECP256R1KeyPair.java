package org.biscuitsec.biscuit.crypto;

import biscuit.format.schema.Schema;
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

class SECP256R1KeyPair extends KeyPair {

    private final java.security.KeyPair keyPair;

    public SECP256R1KeyPair(byte[] bytes) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        var kpg = KeyPairGenerator.getInstance("EC");
        var spec = new ECGenParameterSpec("secp256r1");
        kpg.initialize(spec, new SecureRandom(bytes));
        keyPair = kpg.generateKeyPair();
    }

    public SECP256R1KeyPair(SecureRandom rng) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        byte[] bytes = new byte[32];
        rng.nextBytes(bytes);
        var kpg = KeyPairGenerator.getInstance("EC");
        var spec = new ECGenParameterSpec("secp256r1");
        kpg.initialize(spec, new SecureRandom(bytes));
        keyPair = kpg.generateKeyPair();
    }

    public SECP256R1KeyPair(String hex) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        this(Utils.hexStringToByteArray(hex));
    }

    public static java.security.PublicKey generatePublicKey(byte[] data) throws NoSuchAlgorithmException, InvalidKeySpecException {
        var kf = KeyFactory.getInstance("EC");
        var spec = new X509EncodedKeySpec(data, "SHA256withECDSA");
        return kf.generatePublic(spec);
    }

    public static Signature getSignature() throws NoSuchAlgorithmException {
        return Signature.getInstance("SHA256withECDSA");
    }

    @Override
    public byte[] toBytes() {
        return keyPair.getPublic().getEncoded();
    }

    @Override
    public String toHex() {
        return Utils.byteArrayToHexString(toBytes());
    }

    @Override
    public java.security.PublicKey publicKey() {
        return keyPair.getPublic();
    }

    @Override
    public PrivateKey private_key() {
        return keyPair.getPrivate();
    }

    @Override
    public PublicKey public_key() {
        return new PublicKey(Schema.PublicKey.Algorithm.SECP256R1, keyPair.getPublic());
    }
}
