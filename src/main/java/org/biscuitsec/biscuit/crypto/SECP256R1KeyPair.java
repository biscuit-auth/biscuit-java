package org.biscuitsec.biscuit.crypto;

import biscuit.format.schema.Schema.PublicKey.Algorithm;
import org.biscuitsec.biscuit.token.builder.Utils;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.util.BigIntegers;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Optional;

final class SECP256R1KeyPair extends KeyPair {

    static final int MINIMUM_SIGNATURE_LENGTH = 68;
    static final int MAXIMUM_SIGNATURE_LENGTH = 72;

    private final BCECPrivateKey privateKey;
    private final BCECPublicKey publicKey;
    private final Signer signer;

    private static final String ALGORITHM = "ECDSA";
    private static final String CURVE = "secp256r1";
    private static final ECNamedCurveParameterSpec CURVE_SPEC = ECNamedCurveTable.getParameterSpec(CURVE);

    public SECP256R1KeyPair(byte[] bytes) {
        var privateKeySpec = new ECPrivateKeySpec(BigIntegers.fromUnsignedByteArray(bytes), CURVE_SPEC);
        var privateKey = new BCECPrivateKey(ALGORITHM, privateKeySpec, BouncyCastleProvider.CONFIGURATION);

        var publicKeySpec = new ECPublicKeySpec(CURVE_SPEC.getG().multiply(privateKeySpec.getD()), CURVE_SPEC);
        var publicKey = new BCECPublicKey(ALGORITHM, publicKeySpec, BouncyCastleProvider.CONFIGURATION);

        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.signer = new PrivateKeySigner(Algorithm.SECP256R1, privateKey);
    }

    public SECP256R1KeyPair(SecureRandom rng) {
        byte[] bytes = new byte[32];
        rng.nextBytes(bytes);

        var privateKeySpec = new ECPrivateKeySpec(BigIntegers.fromUnsignedByteArray(bytes), CURVE_SPEC);
        var privateKey = new BCECPrivateKey(ALGORITHM, privateKeySpec, BouncyCastleProvider.CONFIGURATION);

        var publicKeySpec = new ECPublicKeySpec(CURVE_SPEC.getG().multiply(privateKeySpec.getD()), CURVE_SPEC);
        var publicKey = new BCECPublicKey(ALGORITHM, publicKeySpec, BouncyCastleProvider.CONFIGURATION);

        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.signer = new PrivateKeySigner(Algorithm.SECP256R1, privateKey);
    }

    public SECP256R1KeyPair(String hex) {
        this(Utils.hexStringToByteArray(hex));
    }

    public static java.security.PublicKey decode(byte[] data) {
        var params = ECNamedCurveTable.getParameterSpec(CURVE);
        var spec = new ECPublicKeySpec(params.getCurve().decodePoint(data), params);
        return new BCECPublicKey(ALGORITHM, spec, BouncyCastleProvider.CONFIGURATION);
    }

    public static Signature getSignature() throws NoSuchAlgorithmException {
        return Signature.getInstance("SHA256withECDSA", new BouncyCastleProvider());
    }

    @Override
    public byte[] toBytes() {
        return BigIntegers.asUnsignedByteArray(privateKey.getD());
    }

    @Override
    public String toHex() {
        return Utils.byteArrayToHexString(toBytes());
    }

    @Override
    public java.security.PublicKey publicKey() {
        return publicKey;
    }

    @Override
    public byte[] sign(byte[] block, byte[] publicKey) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        return signer.signStandard(block, Algorithm.SECP256R1, publicKey);
    }

    @Override
    public byte[] signExternal(byte[] block, byte[] publicKey, byte[] external) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        return signer.signExternal(block, Algorithm.SECP256R1, publicKey, external);
    }

    @Override
    public byte[] signSealed(byte[] block, byte[] publicKey, byte[] seal) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        return signer.signSealed(block, Algorithm.SECP256R1, publicKey, seal);
    }

    @Override
    public PublicKey public_key() {
        return new PublicKey(Algorithm.SECP256R1, publicKey);
    }
}
