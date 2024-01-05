package org.biscuitsec.biscuit.token.format;

import org.biscuitsec.biscuit.crypto.PublicKey;

public class ExternalSignature {
    public PublicKey key;
    public byte[] signature;

    public ExternalSignature(PublicKey key, byte[] signature) {
        this.key = key;
        this.signature = signature;
    }
}
