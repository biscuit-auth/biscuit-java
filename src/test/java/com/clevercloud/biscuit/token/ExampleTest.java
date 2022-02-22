package com.clevercloud.biscuit.token;

import com.clevercloud.biscuit.crypto.KeyPair;
import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.error.LogicError;
import com.clevercloud.biscuit.token.builder.Block;
import com.clevercloud.biscuit.token.builder.Fact;
import com.clevercloud.biscuit.token.builder.parser.Parser;
import io.vavr.control.Either;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.Set;

import static io.vavr.API.Left;

/* example code for the documentation at https://www.biscuitsec.org
 * if these functions change, please send a PR to update them at https://github.com/biscuit-auth/website
 */
public class ExampleTest {
    public KeyPair root() {
        return new KeyPair();
    }

    public Biscuit createToken(KeyPair root) throws Error {
        return Biscuit.builder(root)
                .add_authority_fact("user(\"1234\")")
                .add_authority_check("check if operation(\"read\")")
                .build();
    }

    public Long authorize(KeyPair root, byte[] serializedToken) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        return Biscuit.from_bytes(serializedToken, root.public_key()).authorizer()
                .add_fact("resource(\"/folder1/file1\")")
                .add_fact("operation(\"read\")")
                .allow()
                .authorize();
    }

    public Biscuit attenuate(KeyPair root, byte[] serializedToken) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        Biscuit token = Biscuit.from_bytes(serializedToken, root.public_key());
        Block block = token.create_block().add_check("check if operation(\"read\")");
        return token.attenuate(block);
    }

    public Set<Fact> query(Authorizer authorizer) throws Error.Timeout, Error.TooManyFacts, Error.TooManyIterations, Error.Parser {
        return authorizer.query("data($name, $id) <- user($name, $id)");
    }
}
