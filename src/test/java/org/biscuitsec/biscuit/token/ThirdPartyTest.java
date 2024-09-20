package org.biscuitsec.biscuit.token;

import org.biscuitsec.biscuit.crypto.KeyPair;
import org.biscuitsec.biscuit.datalog.RunLimits;
import org.biscuitsec.biscuit.error.Error;
import org.biscuitsec.biscuit.error.FailedCheck.FailedBlock;
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
import static org.biscuitsec.biscuit.error.LogicError.MatchedPolicy.*;
import static org.biscuitsec.biscuit.error.LogicError.Unauthorized;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ThirdPartyTest {
    @Test
    public void testRoundTrip() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, Error, IOException {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        out.println("preparing the authority block");

        KeyPair root = new KeyPair(rng);
        KeyPair external = new KeyPair(rng);
        out.println("external: ed25519/" + external.publicKey().toHex());

        Block authorityBuilder = new Block();
        authorityBuilder.addFact("right(\"read\")");
        authorityBuilder.addCheck("check if group(\"admin\") trusting ed25519/" + external.publicKey().toHex());

        Biscuit b1 = Biscuit.make(rng, root, authorityBuilder.build());
        ThirdPartyBlockRequest request = b1.thirdPartyRequest();
        byte[] reqBytes = request.toBytes();
        ThirdPartyBlockRequest reqDeser = ThirdPartyBlockRequest.fromBytes(reqBytes);
        assertEquals(request, reqDeser);

        Block builder = new Block();
        builder.addFact("group(\"admin\")");
        builder.addCheck("check if resource(\"file1\")");

        ThirdPartyBlockContents blockResponse = request.createBlock(external, builder).get();
        byte[] responseBytes = blockResponse.toBytes();
        ThirdPartyBlockContents responseDeser = ThirdPartyBlockContents.fromBytes(responseBytes);
        assertEquals(blockResponse, responseDeser);

        Biscuit b2 = b1.appendThirdPartyBlock(external.publicKey(), blockResponse);

        byte[] data = b2.serialize();
        Biscuit deser = Biscuit.from_bytes(data, root.publicKey());
        assertEquals(b2.print(), deser.print());

        out.println("will check the token for resource=file1");
        Authorizer authorizer = deser.authorizer();
        authorizer.addFact("resource(\"file1\")");
        authorizer.addPolicy("allow if true");
        authorizer.authorize(new RunLimits(500, 100, Duration.ofMillis(500)));

        out.println("will check the token for resource=file2");
        Authorizer authorizer2 = deser.authorizer();
        authorizer2.addFact("resource(\"file2\")");
        authorizer2.addPolicy("allow if true");

        try {
            authorizer2.authorize(new RunLimits(500, 100, Duration.ofMillis(500)));
        } catch (Error e) {
            out.println(e);
            assertEquals(
                    new Error.FailedLogic(new Unauthorized(new Allow(0), List.of(
                            new FailedBlock(1, 0, "check if resource(\"file1\")")
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
        //System.out.println("external: ed25519/"+external.publicKey().toHex());

        Block authorityBuilder = new Block();
        authorityBuilder.addFact("right(\"read\")");
        authorityBuilder.addCheck("check if first(\"admin\") trusting ed25519/" + external1.publicKey().toHex());

        org.biscuitsec.biscuit.token.Block authority_block = authorityBuilder.build();
        out.println(authority_block);
        Biscuit b1 = Biscuit.make(rng, root, authority_block);
        out.println("TOKEN: " + b1.print());

        ThirdPartyBlockRequest request1 = b1.thirdPartyRequest();
        Block builder = new Block();
        builder.addFact("first(\"admin\")");
        builder.addFact("second(\"A\")");
        builder.addCheck("check if third(3) trusting ed25519/" + external2.publicKey().toHex());
        ThirdPartyBlockContents blockResponse = request1.createBlock(external1, builder).get();
        Biscuit b2 = b1.appendThirdPartyBlock(external1.publicKey(), blockResponse);
        byte[] data = b2.serialize();
        Biscuit deser2 = Biscuit.from_bytes(data, root.publicKey());
        assertEquals(b2.print(), deser2.print());
        out.println("TOKEN: " + deser2.print());

        ThirdPartyBlockRequest request2 = deser2.thirdPartyRequest();
        Block builder2 = new Block();
        builder2.addFact("third(3)");
        builder2.addCheck("check if fourth(1) trusting ed25519/" + external3.publicKey().toHex() + ", ed25519/" + external1.publicKey().toHex());
        ThirdPartyBlockContents blockResponse2 = request2.createBlock(external2, builder2).get();
        Biscuit b3 = deser2.appendThirdPartyBlock(external2.publicKey(), blockResponse2);
        byte[] data2 = b3.serialize();
        Biscuit deser3 = Biscuit.from_bytes(data2, root.publicKey());
        assertEquals(b3.print(), deser3.print());
        out.println("TOKEN: " + deser3.print());


        ThirdPartyBlockRequest request3 = deser3.thirdPartyRequest();
        Block builder3 = new Block();
        builder3.addFact("fourth(1)");
        builder3.addCheck("check if resource(\"file1\")");
        ThirdPartyBlockContents blockResponse3 = request3.createBlock(external1, builder3).get();
        Biscuit b4 = deser3.appendThirdPartyBlock(external1.publicKey(), blockResponse3);
        byte[] data3 = b4.serialize();
        Biscuit deser4 = Biscuit.from_bytes(data3, root.publicKey());
        assertEquals(b4.print(), deser4.print());
        out.println("TOKEN: " + deser4.print());

        out.println("will check the token for resource=file1");
        Authorizer authorizer = deser4.authorizer();
        authorizer.addFact("resource(\"file1\")");
        authorizer.addPolicy("allow if true");
        out.println("Authorizer world:\n" + authorizer.printWorld());
        authorizer.authorize(new RunLimits(500, 100, Duration.ofMillis(500)));

        out.println("will check the token for resource=file2");
        Authorizer authorizer2 = deser4.authorizer();
        authorizer2.addFact("resource(\"file2\")");
        authorizer2.addPolicy("allow if true");
        out.println("Authorizer world 2:\n" + authorizer2.printWorld());

        try {
            authorizer2.authorize(new RunLimits(500, 100, Duration.ofMillis(500)));
        } catch (Error e) {
            out.println(e);
            assertEquals(
                    new Error.FailedLogic(new Unauthorized(new Allow(0), List.of(
                            new FailedBlock(3, 0, "check if resource(\"file1\")")
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
        out.println("external: ed25519/" + external.publicKey().toHex());

        Block authorityBuilder = new Block();
        authorityBuilder.addFact("right(\"read\")");
        authorityBuilder.addCheck("check if group(\"admin\") trusting ed25519/" + external.publicKey().toHex());

        Biscuit b1 = Biscuit.make(rng, root, authorityBuilder.build());
        ThirdPartyBlockRequest request = b1.thirdPartyRequest();
        Block builder = new Block();
        builder.addFact("group(\"admin\")");
        builder.addFact("resource(\"file2\")");
        builder.addCheck("check if resource(\"file1\")");
        builder.addCheck("check if right(\"read\")");

        ThirdPartyBlockContents blockResponse = request.createBlock(external, builder).get();
        Biscuit b2 = b1.appendThirdPartyBlock(external.publicKey(), blockResponse);

        byte[] data = b2.serialize();
        Biscuit deser = Biscuit.from_bytes(data, root.publicKey());
        assertEquals(b2.print(), deser.print());

        out.println("will check the token for resource=file1");
        Authorizer authorizer = deser.authorizer();
        authorizer.addFact("resource(\"file1\")");
        authorizer.addPolicy("allow if true");
        authorizer.authorize(new RunLimits(500, 100, Duration.ofMillis(500)));
        out.println("Authorizer world:\n" + authorizer.printWorld());


        out.println("will check the token for resource=file2");
        Authorizer authorizer2 = deser.authorizer();
        authorizer2.addFact("resource(\"file2\")");
        authorizer2.addPolicy("allow if true");

        try {
            authorizer2.authorize(new RunLimits(500, 100, Duration.ofMillis(500)));
        } catch (Error e) {
            out.println(e);
            assertEquals(
                    new Error.FailedLogic(new Unauthorized(new Allow(0), List.of(
                            new FailedBlock(1, 0, "check if resource(\"file1\")")
                    ))),
                    e);
        }
    }
}

