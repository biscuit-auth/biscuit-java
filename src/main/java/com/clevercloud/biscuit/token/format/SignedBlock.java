package com.clevercloud.biscuit.token.format;

import com.clevercloud.biscuit.crypto.KeyPair;
import com.clevercloud.biscuit.crypto.PublicKey;

import java.util.List;

public class SignedBlock {
    public byte[] block;
    public PublicKey key;
    public byte[] signature;

    public SignedBlock(byte[] block, PublicKey key, byte[] signature) {
        this.block = block;
        this.key = key;
        this.signature = signature;
    }
}
