package com.clevercloud.biscuit.token;

import com.clevercloud.biscuit.crypto.KeyPair;
import com.clevercloud.biscuit.error.Error;
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

    public Either<Error, Biscuit> createToken(KeyPair root) {
        com.clevercloud.biscuit.token.builder.Biscuit builder = Biscuit.builder(root);

        Either<Error, Void> res = builder.add_authority_fact("user(\"1234\")");
        if (res.isLeft()) {
            Error e = res.getLeft();
            return Left(e);
        }

        res = builder.add_authority_check("check if operation(\"read\")");
        if (res.isLeft()) {
            Error e = res.getLeft();
            return Left(e);
        }

        return builder.build();
    }

    public Either<Error, Long> authorize(KeyPair root, byte[] serializedToken) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        Either<Error, Biscuit> res = Biscuit.from_bytes(serializedToken, root.public_key());
        if (res.isLeft()) {
            Error e = res.getLeft();
            return Left(e);
        }

        Biscuit token = res.get();

        Either<Error, Authorizer> authorizerRes = token.authorizer();
        if (authorizerRes.isLeft()) {
            Error e = authorizerRes.getLeft();
            return Left(e);
        }

        Authorizer authorizer = authorizerRes.get();
        Either<Error, Void> addRes = authorizer.add_fact("resource(\"/folder1/file1\")");
        if (addRes.isLeft()) {
            Error e = res.getLeft();
            return Left(e);
        }

        addRes = authorizer.add_fact("operation(\"read\")");
        if (addRes.isLeft()) {
            Error e = res.getLeft();
            return Left(e);
        }

        authorizer.allow();

        return authorizer.authorize();
    }

    public Either<Error, Biscuit> attenuate(KeyPair root, byte[] serializedToken) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        Either<Error, Biscuit> res = Biscuit.from_bytes(serializedToken, root.public_key());
        if (res.isLeft()) {
            Error e = res.getLeft();
            return Left(e);
        }

        Biscuit token = res.get();
        Block block = token.create_block();
        Either<Error, Void> addRes = block.add_check("check if operation(\"read\")");
        if (addRes.isLeft()) {
            Error e = res.getLeft();
            return Left(e);
        }

        return token.attenuate(block);
    }

    public Either<Error, Set<Fact>> query(Authorizer authorizer) {
       return authorizer.query("data($name, $id) <- user($name, $id)");
    }
}
