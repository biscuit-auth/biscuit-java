package org.biscuitsec.biscuit.token;

import java.util.Base64;

public class RevocationIdentifier {
    private byte[] bytes;

    public RevocationIdentifier(byte[] bytes) {
        this.bytes = bytes;
    }

    /**
     * Creates a RevocationIdentifier from base64 url (RFC4648_URLSAFE)
     * @param b64url serialized revocation identifier
     * @return RevocationIdentifier
     */
    public static RevocationIdentifier from_b64url(String b64url) {
        return new RevocationIdentifier(Base64.getDecoder().decode(b64url));
    }

    /**
     * Serializes a revocation identifier as base64 url (RFC4648_URLSAFE)
     * @return String
     */
    public String serialize_b64url() {
        return Base64.getEncoder().encodeToString(this.bytes);
    }

    public static RevocationIdentifier from_bytes(byte[] bytes) {
        return new RevocationIdentifier(bytes);
    }

    public byte[] getBytes() {
        return this.bytes;
    }
}
