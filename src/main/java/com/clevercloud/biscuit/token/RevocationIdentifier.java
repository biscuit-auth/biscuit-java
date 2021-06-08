package com.clevercloud.biscuit.token;

import java.util.Base64;

public class RevocationIdentifier {
    private byte[] bytes;

    public RevocationIdentifier(byte[] bytes) {
        this.bytes = bytes;
    }

    public static RevocationIdentifier from_b64(String b64) {
        return new RevocationIdentifier(Base64.getDecoder().decode(b64));
    }

    public String serialize_b64() {
        return Base64.getEncoder().encodeToString(this.bytes);
    }

    public static RevocationIdentifier from_bytes(byte[] bytes) {
        return new RevocationIdentifier(bytes);
    }

    public byte[] getBytes() {
        return this.bytes;
    }
}
