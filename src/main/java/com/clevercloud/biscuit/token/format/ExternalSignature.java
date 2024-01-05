package com.clevercloud.biscuit.token.format;

import com.clevercloud.biscuit.crypto.PublicKey;

public class ExternalSignature {
    public PublicKey key;
    public byte[] signature;

    public ExternalSignature(PublicKey key, byte[] signature) {
        this.key = key;
        this.signature = signature;
    }
}
