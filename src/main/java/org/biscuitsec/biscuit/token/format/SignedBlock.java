package org.biscuitsec.biscuit.token.format;

import org.biscuitsec.biscuit.crypto.PublicKey;
import io.vavr.control.Option;

public class SignedBlock {
    public final byte[] block;
    public final PublicKey key;
    public final byte[] signature;
    public final Option<ExternalSignature> externalSignature;

    public SignedBlock(byte[] block, PublicKey key, byte[] signature, Option<ExternalSignature> externalSignature) {
        this.block = block;
        this.key = key;
        this.signature = signature;
        this.externalSignature = externalSignature;
    }
}
