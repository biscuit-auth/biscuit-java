package org.biscuitsec.biscuit.token;

import org.biscuitsec.biscuit.crypto.PublicKey;

public class ThirdPartyBlock {
    byte[] payload;
    byte[] signature;
    PublicKey publicKey;

    ThirdPartyBlock(byte[] payload, byte[] signature, PublicKey publicKey) {
        this.payload = payload;
        this.signature = signature;
        this.publicKey = publicKey;
    }
}
