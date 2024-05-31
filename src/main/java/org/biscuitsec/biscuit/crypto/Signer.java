package org.biscuitsec.biscuit.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

public interface Signer {
    byte[] sign(byte[] bytes) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException;
}
