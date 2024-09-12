package org.biscuitsec.biscuit.token;

import org.biscuitsec.biscuit.crypto.KeyPair;
import org.biscuitsec.biscuit.datalog.RunLimits;
import org.biscuitsec.biscuit.error.Error;
import org.biscuitsec.biscuit.error.FailedCheck;
import org.biscuitsec.biscuit.error.LogicError;
import org.biscuitsec.biscuit.token.builder.Block;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.time.Duration;
import java.util.List;

import static java.lang.System.out;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ThirdPartyTest {
    @Test
    public void testRoundTrip() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error, IOException {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        out.println("preparing the authority block");

        KeyPair root = new KeyPair(rng);
        KeyPair external = new KeyPair(rng);
        out.println("external: ed25519/" + external.public_key().toHex());

        Block authorityBuilder = new Block();
        authorityBuilder.add_fact("right(\"read\")");
        authorityBuilder.add_check("check if group(\"admin\") trusting ed25519/" + external.public_key().toHex());

        Biscuit b1 = Biscuit.make(rng, root, authorityBuilder.build());
        ThirdPartyBlockRequest request = b1.thirdPartyRequest();
        byte[] reqBytes = request.toBytes();
        ThirdPartyBlockRequest reqDeser = ThirdPartyBlockRequest.fromBytes(reqBytes);
        assertEquals(request, reqDeser);

        Block builder = new Block();
        builder.add_fact("group(\"admin\")");
        builder.add_check("check if resource(\"file1\")");

        ThirdPartyBlockContents blockResponse = request.createBlock(external, builder).get();
        byte[] responseBytes = blockResponse.toBytes();
        ThirdPartyBlockContents responseDeser = ThirdPartyBlockContents.fromBytes(responseBytes);
        assertEquals(blockResponse, responseDeser);

        Biscuit b2 = b1.appendThirdPartyBlock(external.public_key(), blockResponse);

        byte[] data = b2.serialize();
        Biscuit deser = Biscuit.from_bytes(data, root.public_key());
        assertEquals(b2.print(), deser.print());

        out.println("will check the token for resource=file1");
        Authorizer authorizer = deser.authorizer();
        authorizer.add_fact("resource(\"file1\")");
        authorizer.add_policy("allow if true");
        authorizer.authorize(new RunLimits(500, 100, Duration.ofMillis(500)));

        out.println("will check the token for resource=file2");
        Authorizer authorizer2 = deser.authorizer();
        authorizer2.add_fact("resource(\"file2\")");
        authorizer2.add_policy("allow if true");

        try {
            authorizer2.authorize(new RunLimits(500, 100, Duration.ofMillis(500)));
        } catch (Error e) {
            out.println(e);
            assertEquals(
                    new Error.FailedLogic(new LogicError.Unauthorized(new LogicError.MatchedPolicy.Allow(0), List.of(
                            new FailedCheck.FailedBlock(1, 0, "check if resource(\"file1\")")
                    ))),
                    e);
        }
    }

    @Test
    public void testPublicKeyInterning() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        // this makes a deterministic RNG
        SecureRandom rng = SecureRandom.getInstance("SHA1PRNG");
        byte[] seed = {0, 0, 0, 0};
        rng.setSeed(seed);

        out.println("preparing the authority block");

        KeyPair root = new KeyPair(rng);
        KeyPair external1 = new KeyPair(rng);
        KeyPair external2 = new KeyPair(rng);
        KeyPair external3 = new KeyPair(rng);
        //System.out.println("external: ed25519/"+external.public_key().toHex());

        Block authorityBuilder = new Block();
        authorityBuilder.add_fact("right(\"read\")");
        authorityBuilder.add_check("check if first(\"admin\") trusting ed25519/" + external1.public_key().toHex());

        org.biscuitsec.biscuit.token.Block authority_block = authorityBuilder.build();
        out.println(authority_block);
        Biscuit b1 = Biscuit.make(rng, root, authority_block);
        out.println("TOKEN: " + b1.print());

        ThirdPartyBlockRequest request1 = b1.thirdPartyRequest();
        Block builder = new Block();
        builder.add_fact("first(\"admin\")");
        builder.add_fact("second(\"A\")");
        builder.add_check("check if third(3) trusting ed25519/" + external2.public_key().toHex());
        ThirdPartyBlockContents blockResponse = request1.createBlock(external1, builder).get();
        Biscuit b2 = b1.appendThirdPartyBlock(external1.public_key(), blockResponse);
        byte[] data = b2.serialize();
        Biscuit deser2 = Biscuit.from_bytes(data, root.public_key());
        assertEquals(b2.print(), deser2.print());
        out.println("TOKEN: " + deser2.print());

        ThirdPartyBlockRequest request2 = deser2.thirdPartyRequest();
        Block builder2 = new Block();
        builder2.add_fact("third(3)");
        builder2.add_check("check if fourth(1) trusting ed25519/" + external3.public_key().toHex() + ", ed25519/" + external1.public_key().toHex());
        ThirdPartyBlockContents blockResponse2 = request2.createBlock(external2, builder2).get();
        Biscuit b3 = deser2.appendThirdPartyBlock(external2.public_key(), blockResponse2);
        byte[] data2 = b3.serialize();
        Biscuit deser3 = Biscuit.from_bytes(data2, root.public_key());
        assertEquals(b3.print(), deser3.print());
        out.println("TOKEN: " + deser3.print());


        ThirdPartyBlockRequest request3 = deser3.thirdPartyRequest();
        Block builder3 = new Block();
        builder3.add_fact("fourth(1)");
        builder3.add_check("check if resource(\"file1\")");
        ThirdPartyBlockContents blockResponse3 = request3.createBlock(external1, builder3).get();
        Biscuit b4 = deser3.appendThirdPartyBlock(external1.public_key(), blockResponse3);
        byte[] data3 = b4.serialize();
        Biscuit deser4 = Biscuit.from_bytes(data3, root.public_key());
        assertEquals(b4.print(), deser4.print());
        out.println("TOKEN: " + deser4.print());

        out.println("will check the token for resource=file1");
        Authorizer authorizer = deser4.authorizer();
        authorizer.add_fact("resource(\"file1\")");
        authorizer.add_policy("allow if true");
        out.println("Authorizer world:\n" + authorizer.print_world());
        authorizer.authorize(new RunLimits(500, 100, Duration.ofMillis(500)));

        out.println("will check the token for resource=file2");
        Authorizer authorizer2 = deser4.authorizer();
        authorizer2.add_fact("resource(\"file2\")");
        authorizer2.add_policy("allow if true");
        out.println("Authorizer world 2:\n" + authorizer2.print_world());

        try {
            authorizer2.authorize(new RunLimits(500, 100, Duration.ofMillis(500)));
        } catch (Error e) {
            out.println(e);
            assertEquals(
                    new Error.FailedLogic(new LogicError.Unauthorized(new LogicError.MatchedPolicy.Allow(0), List.of(
                            new FailedCheck.FailedBlock(3, 0, "check if resource(\"file1\")")
                    ))),
                    e);
        }
    }

    @Test
    public void testReusedSymbols() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        out.println("preparing the authority block");

        KeyPair root = new KeyPair(rng);
        KeyPair external = new KeyPair(rng);
        out.println("external: ed25519/" + external.public_key().toHex());

        Block authorityBuilder = new Block();
        authorityBuilder.add_fact("right(\"read\")");
        authorityBuilder.add_check("check if group(\"admin\") trusting ed25519/" + external.public_key().toHex());

        Biscuit b1 = Biscuit.make(rng, root, authorityBuilder.build());
        ThirdPartyBlockRequest request = b1.thirdPartyRequest();
        Block builder = new Block();
        builder.add_fact("group(\"admin\")");
        builder.add_fact("resource(\"file2\")");
        builder.add_check("check if resource(\"file1\")");
        builder.add_check("check if right(\"read\")");

        ThirdPartyBlockContents blockResponse = request.createBlock(external, builder).get();
        Biscuit b2 = b1.appendThirdPartyBlock(external.public_key(), blockResponse);

        byte[] data = b2.serialize();
        Biscuit deser = Biscuit.from_bytes(data, root.public_key());
        assertEquals(b2.print(), deser.print());

        out.println("will check the token for resource=file1");
        Authorizer authorizer = deser.authorizer();
        authorizer.add_fact("resource(\"file1\")");
        authorizer.add_policy("allow if true");
        authorizer.authorize(new RunLimits(500, 100, Duration.ofMillis(500)));
        out.println("Authorizer world:\n" + authorizer.print_world());


        out.println("will check the token for resource=file2");
        Authorizer authorizer2 = deser.authorizer();
        authorizer2.add_fact("resource(\"file2\")");
        authorizer2.add_policy("allow if true");

        try {
            authorizer2.authorize(new RunLimits(500, 100, Duration.ofMillis(500)));
        } catch (Error e) {
            out.println(e);
            assertEquals(
                    new Error.FailedLogic(new LogicError.Unauthorized(new LogicError.MatchedPolicy.Allow(0), List.of(
                            new FailedCheck.FailedBlock(1, 0, "check if resource(\"file1\")")
                    ))),
                    e);
        }
    }
}

