package org.biscuitsec.biscuit.token;

import biscuit.format.schema.Schema;
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
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ThirdPartyTest {
    @Test
    public void testRoundTrip() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CloneNotSupportedException, Error, IOException {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        System.out.println("preparing the authority block");

        KeyPair root = KeyPair.generate(Schema.PublicKey.Algorithm.Ed25519, rng);
        KeyPair external = KeyPair.generate(Schema.PublicKey.Algorithm.Ed25519, rng);
        System.out.println("external: ed25519/"+external.public_key().toHex());

        Block authority_builder = new Block();
        authority_builder.add_fact("right(\"read\")");
        authority_builder.add_check("check if group(\"admin\") trusting ed25519/"+external.public_key().toHex());

        Biscuit b1 = Biscuit.make(rng, root, authority_builder.build());
        ThirdPartyBlockRequest request = b1.thirdPartyRequest();
        byte[] reqb = request.toBytes();
        ThirdPartyBlockRequest reqdeser = ThirdPartyBlockRequest.fromBytes(reqb);
        assertEquals(request, reqdeser);

        Block builder = new Block();
        builder.add_fact("group(\"admin\")");
        builder.add_check("check if resource(\"file1\")");

        ThirdPartyBlockContents blockResponse = request.createBlock(external, builder).get();
        byte[] resb = blockResponse.toBytes();
        ThirdPartyBlockContents resdeser = ThirdPartyBlockContents.fromBytes(resb);
        assertEquals(blockResponse, resdeser);

        Biscuit b2 = b1.appendThirdPartyBlock(external.public_key(), blockResponse);

        byte[] data = b2.serialize();
        Biscuit deser = Biscuit.from_bytes(data, root.public_key());
        assertEquals(b2.print(), deser.print());

        System.out.println("will check the token for resource=file1");
        Authorizer authorizer = deser.authorizer();
        authorizer.add_fact("resource(\"file1\")");
        authorizer.add_policy("allow if true");
        authorizer.authorize(new RunLimits(500, 100, Duration.ofMillis(500)));

        System.out.println("will check the token for resource=file2");
        Authorizer authorizer2 = deser.authorizer();
        authorizer2.add_fact("resource(\"file2\")");
        authorizer2.add_policy("allow if true");

        try {
            authorizer2.authorize(new RunLimits(500, 100, Duration.ofMillis(500)));
        } catch (Error e) {
            System.out.println(e);
            assertEquals(
                    new Error.FailedLogic(new LogicError.Unauthorized(new LogicError.MatchedPolicy.Allow(0), Arrays.asList(
                            new FailedCheck.FailedBlock(1, 0, "check if resource(\"file1\")")
                    ))),
                    e);
        }
    }

    @Test
    public void testPublicKeyInterning() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CloneNotSupportedException, Error {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        System.out.println("preparing the authority block");

        KeyPair root = KeyPair.generate(Schema.PublicKey.Algorithm.Ed25519, rng);
        KeyPair external1 = KeyPair.generate(Schema.PublicKey.Algorithm.Ed25519, rng);
        KeyPair external2 = KeyPair.generate(Schema.PublicKey.Algorithm.Ed25519, rng);
        KeyPair external3 = KeyPair.generate(Schema.PublicKey.Algorithm.Ed25519, rng);
        //System.out.println("external: ed25519/"+external.public_key().toHex());

        Block authority_builder = new Block();
        authority_builder.add_fact("right(\"read\")");
        authority_builder.add_check("check if first(\"admin\") trusting ed25519/"+external1.public_key().toHex());

        org.biscuitsec.biscuit.token.Block authority_block =  authority_builder.build();
        System.out.println(authority_block);
        Biscuit b1 = Biscuit.make(rng, root, authority_block);
        System.out.println("TOKEN: "+b1.print());

        ThirdPartyBlockRequest request1 = b1.thirdPartyRequest();
        Block builder = new Block();
        builder.add_fact("first(\"admin\")");
        builder.add_fact("second(\"A\")");
        builder.add_check("check if third(3) trusting ed25519/"+external2.public_key().toHex());
        ThirdPartyBlockContents blockResponse = request1.createBlock(external1, builder).get();
        Biscuit b2 = b1.appendThirdPartyBlock(external1.public_key(), blockResponse);
        byte[] data = b2.serialize();
        Biscuit deser2 = Biscuit.from_bytes(data, root.public_key());
        assertEquals(b2.print(), deser2.print());
        System.out.println("TOKEN: "+deser2.print());

        ThirdPartyBlockRequest request2 = deser2.thirdPartyRequest();
        Block builder2 = new Block();
        builder2.add_fact("third(3)");
        builder2.add_check("check if fourth(1) trusting ed25519/"+external3.public_key().toHex()+", ed25519/"+external1.public_key().toHex());
        ThirdPartyBlockContents blockResponse2 = request2.createBlock(external2, builder2).get();
        Biscuit b3 = deser2.appendThirdPartyBlock(external2.public_key(), blockResponse2);
        byte[] data2 = b3.serialize();
        Biscuit deser3 = Biscuit.from_bytes(data2, root.public_key());
        assertEquals(b3.print(), deser3.print());
        System.out.println("TOKEN: "+deser3.print());


        ThirdPartyBlockRequest request3 = deser3.thirdPartyRequest();
        Block builder3 = new Block();
        builder3.add_fact("fourth(1)");
        builder3.add_check("check if resource(\"file1\")");
        ThirdPartyBlockContents blockResponse3 = request3.createBlock(external1, builder3).get();
        Biscuit b4 = deser3.appendThirdPartyBlock(external1.public_key(), blockResponse3);
        byte[] data3 = b4.serialize();
        Biscuit deser4 = Biscuit.from_bytes(data3, root.public_key());
        assertEquals(b4.print(), deser4.print());
        System.out.println("TOKEN: "+deser4.print());


        System.out.println("will check the token for resource=file1");
        Authorizer authorizer = deser4.authorizer();
        authorizer.add_fact("resource(\"file1\")");
        authorizer.add_policy("allow if true");
        System.out.println("Authorizer world:\n"+authorizer.print_world());
        authorizer.authorize(new RunLimits(500, 100, Duration.ofMillis(500)));

        System.out.println("will check the token for resource=file2");
        Authorizer authorizer2 = deser4.authorizer();
        authorizer2.add_fact("resource(\"file2\")");
        authorizer2.add_policy("allow if true");
        System.out.println("Authorizer world 2:\n"+authorizer2.print_world());

        try {
            authorizer2.authorize(new RunLimits(500, 100, Duration.ofMillis(500)));
        } catch (Error e) {
            System.out.println(e);
            assertEquals(
                    new Error.FailedLogic(new LogicError.Unauthorized(new LogicError.MatchedPolicy.Allow(0), Arrays.asList(
                            new FailedCheck.FailedBlock(3, 0, "check if resource(\"file1\")")
                    ))),
                    e);
        }
    }

    @Test
    public void testReusedSymbols() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CloneNotSupportedException, Error {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        System.out.println("preparing the authority block");

        KeyPair root = KeyPair.generate(Schema.PublicKey.Algorithm.Ed25519, rng);
        KeyPair external = KeyPair.generate(Schema.PublicKey.Algorithm.Ed25519, rng);
        System.out.println("external: ed25519/"+external.public_key().toHex());

        Block authority_builder = new Block();
        authority_builder.add_fact("right(\"read\")");
        authority_builder.add_check("check if group(\"admin\") trusting ed25519/"+external.public_key().toHex());

        Biscuit b1 = Biscuit.make(rng, root, authority_builder.build());
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

        System.out.println("will check the token for resource=file1");
        Authorizer authorizer = deser.authorizer();
        authorizer.add_fact("resource(\"file1\")");
        authorizer.add_policy("allow if true");
        authorizer.authorize(new RunLimits(500, 100, Duration.ofMillis(500)));
        System.out.println("Authorizer world:\n"+authorizer.print_world());


        System.out.println("will check the token for resource=file2");
        Authorizer authorizer2 = deser.authorizer();
        authorizer2.add_fact("resource(\"file2\")");
        authorizer2.add_policy("allow if true");

        try {
            authorizer2.authorize(new RunLimits(500, 100, Duration.ofMillis(500)));
        } catch (Error e) {
            System.out.println(e);
            assertEquals(
                    new Error.FailedLogic(new LogicError.Unauthorized(new LogicError.MatchedPolicy.Allow(0), Arrays.asList(
                            new FailedCheck.FailedBlock(1, 0, "check if resource(\"file1\")")
                    ))),
                    e);
        }
    }
}

