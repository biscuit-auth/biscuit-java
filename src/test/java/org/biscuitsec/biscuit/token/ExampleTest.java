package org.biscuitsec.biscuit.token;

import org.biscuitsec.biscuit.crypto.KeyPair;
import org.biscuitsec.biscuit.error.Error;
import org.biscuitsec.biscuit.token.builder.Block;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

/* example code for the documentation at https://www.biscuitsec.org
 * if these functions change, please send a PR to update them at https://github.com/biscuit-auth/website
 */
public class ExampleTest {
    public KeyPair root() {
        return new KeyPair();
    }

    public Biscuit createToken(KeyPair root) throws Error {
        return Biscuit.builder(root)
                .addAuthorityFact("user(\"1234\")")
                .addAuthorityCheck("check if operation(\"read\")")
                .build();
    }

    public Long authorize(KeyPair root, byte[] serializedToken) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        return Biscuit.from_bytes(serializedToken, root.publicKey()).authorizer()
                .addFact("resource(\"/folder1/file1\")")
                .addFact("operation(\"read\")")
                .allow()
                .authorize();
    }

    public Biscuit attenuate(KeyPair root, byte[] serializedToken) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        Biscuit token = Biscuit.from_bytes(serializedToken, root.publicKey());
        Block block = token.create_block().addCheck("check if operation(\"read\")");
        return token.attenuate(block);
    }

    /*public Set<Fact> query(Authorizer authorizer) throws Error.Timeout, Error.TooManyFacts, Error.TooManyIterations, Error.Parser {
        return authorizer.query("data($name, $id) <- user($name, $id)");
    }*/
}
