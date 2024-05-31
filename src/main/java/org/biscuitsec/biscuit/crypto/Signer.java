package org.biscuitsec.biscuit.crypto;

import biscuit.format.schema.Schema.PublicKey.Algorithm;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

public interface Signer {
    byte[] sign(byte[] block, Algorithm algorithm, byte[] publicKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException;
    byte[] sign(byte[] block, Algorithm algorithm, byte[] publicKey, byte[] seal) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException;

    static byte[] getAlgorithmBuffer(Algorithm algorithm) {
        var algorithmBuffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        algorithmBuffer.putInt(algorithm.getNumber());
        algorithmBuffer.flip();
        return algorithmBuffer.array();
    }
}
