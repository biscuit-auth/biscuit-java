package org.biscuitsec.biscuit.token.format;

import org.biscuitsec.biscuit.crypto.PublicKey;

public class ExternalSignature {
    public final PublicKey key;
    public final byte[] signature;

    public ExternalSignature(PublicKey key, byte[] signature) {
        this.key = key;
        this.signature = signature;
    }
}
