package org.biscuitsec.biscuit.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Optional;

public class RemoteKeyPair extends KeyPair {

    private final org.biscuitsec.biscuit.crypto.PublicKey publicKey;
    private final Signer signer;

    public RemoteKeyPair(org.biscuitsec.biscuit.crypto.PublicKey publicKey, Signer signer) {
        this.publicKey = publicKey;
        this.signer = signer;
    }

    @Override
    public byte[] toBytes() {
        throw new RuntimeException("Illegal operation; remote private private key cannot be accessed.");
    }

    @Override
    public String toHex() {
        throw new RuntimeException("Illegal operation; remote private private key cannot be accessed.");
    }

    @Override
    public PublicKey publicKey() {
        return publicKey.key;
    }

    @Override
    public byte[] sign(byte[] block, byte[] publicKey) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        return signer.signStandard(block, this.publicKey.algorithm, publicKey);
    }

    @Override
    public byte[] signExternal(byte[] block, byte[] publicKey, byte[] external) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        return signer.signExternal(block, this.publicKey.algorithm, publicKey, external);
    }

    @Override
    public byte[] signSealed(byte[] block, byte[] publicKey, byte[] seal) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        return signer.signSealed(block, this.publicKey.algorithm, publicKey, seal);
    }

    @Override
    public org.biscuitsec.biscuit.crypto.PublicKey public_key() {
        return publicKey;
    }
}
