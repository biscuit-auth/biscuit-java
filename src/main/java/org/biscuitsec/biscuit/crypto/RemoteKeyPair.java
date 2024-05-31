package org.biscuitsec.biscuit.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;

public class RemoteKeyPair extends KeyPair {

    private final org.biscuitsec.biscuit.crypto.PublicKey publicKey;
    private final Signer signer;

    public RemoteKeyPair(org.biscuitsec.biscuit.crypto.PublicKey publicKey, Signer signer) {
        this.publicKey = publicKey;
        this.signer = signer;
    }

    @Override
    public byte[] toBytes() {
        throw new RuntimeException("Illegal operation; remote private private key cannot be retrieved.");
    }

    @Override
    public String toHex() {
        throw new RuntimeException("Illegal operation; remote private private key cannot be retrieved.");
    }

    @Override
    public PublicKey publicKey() {
        return publicKey.key;
    }

    @Override
    public byte[] sign(byte[] block, byte[] publicKey) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        return signer.sign(block, this.publicKey.algorithm, publicKey);
    }

    @Override
    public byte[] sign(byte[] block, byte[] publicKey, byte[] signature) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        return signer.sign(block, this.publicKey.algorithm, publicKey, signature);
    }

    @Override
    public org.biscuitsec.biscuit.crypto.PublicKey public_key() {
        return publicKey;
    }
}
