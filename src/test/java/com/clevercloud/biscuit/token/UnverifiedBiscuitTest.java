package com.clevercloud.biscuit.token;

import com.clevercloud.biscuit.crypto.KeyPair;
import com.clevercloud.biscuit.datalog.Fact;
import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.error.FailedCheck;
import com.clevercloud.biscuit.error.LogicError;
import com.clevercloud.biscuit.token.builder.Block;
import org.junit.jupiter.api.Test;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.clevercloud.biscuit.token.builder.Utils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class UnverifiedBiscuitTest {

    @Test
    public void testBasic() throws Error, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        System.out.println("preparing the authority block, block0");

        KeyPair keypair0 = new KeyPair(rng);

        SymbolTable symbols = Biscuit.default_symbol_table();
        Block block0 = new Block(0, symbols);
        block0.add_fact(fact("right", List.of(s("file1"), s("read"))));
        block0.add_fact(fact("right", List.of(s("file2"), s("read"))));
        block0.add_fact(fact("right", List.of(s("file1"), s("write"))));

        Biscuit biscuit0 = Biscuit.make(rng, keypair0, Biscuit.default_symbol_table(), block0.build());

        System.out.println(biscuit0.print());
        System.out.println("serializing the first token");

        String data = biscuit0.serialize_b64url();

        System.out.print("data len: ");
        System.out.println(data.length());
        System.out.println(data);

        System.out.println("deserializing the first token");
        UnverifiedBiscuit deser0 = UnverifiedBiscuit.from_b64url(data);
        System.out.println(deser0.print());

        // SECOND BLOCK
        System.out.println("preparing the second block");

        KeyPair keypair1 = new KeyPair(rng);
        Block block1 = deser0.create_block();
        block1.add_check(check(rule(
                "caveat1",
                List.of(var("resource")),
                List.of(
                        pred("resource", List.of(var("resource"))),
                        pred("operation", List.of(s("read"))),
                        pred("right", List.of(var("resource"), s("read")))
                )
        )));
        UnverifiedBiscuit unverifiedBiscuit1 = deser0.attenuate(rng, keypair1, block1.build());

        System.out.println(unverifiedBiscuit1.print());

        System.out.println("serializing the second token");

        String data1 = unverifiedBiscuit1.serialize_b64url();

        System.out.print("data len: ");
        System.out.println(data1.length());
        System.out.println(data1);

        System.out.println("deserializing the second token");
        UnverifiedBiscuit deser1 = UnverifiedBiscuit.from_b64url(data1);

        System.out.println(deser1.print());

        // THIRD BLOCK
        System.out.println("preparing the third block");

        KeyPair keypair2 = new KeyPair(rng);

        Block block2 = unverifiedBiscuit1.create_block();
        block2.add_check(check(rule(
                "caveat2",
                List.of(s("file1")),
                List.of(
                        pred("resource", List.of(s("file1")))
                )
        )));

        UnverifiedBiscuit unverifiedBiscuit2 = unverifiedBiscuit1.attenuate(rng, keypair2, block2.build());

        System.out.println(unverifiedBiscuit2.print());

        System.out.println("serializing the third token");

        String data2 = unverifiedBiscuit2.serialize_b64url();

        System.out.print("data len: ");
        System.out.println(data2.length());
        System.out.println(data2);

        System.out.println("deserializing the third token");
        UnverifiedBiscuit finalUnverifiedBiscuit = UnverifiedBiscuit.from_b64url(data2);

        System.out.println(finalUnverifiedBiscuit.print());

        // Crate Biscuit from UnverifiedBiscuit
        Biscuit finalBiscuit = finalUnverifiedBiscuit.verify(keypair0.public_key());

        // check
        System.out.println("will check the token for resource=file1 and operation=read");

        SymbolTable check_symbols = new SymbolTable(finalBiscuit.symbols);
        List<Fact> ambient_facts = List.of(
                fact("resource", List.of(s("file1"))).convert(check_symbols),
                fact("operation", List.of(s("read"))).convert(check_symbols)
        );

        finalBiscuit.check(check_symbols, ambient_facts,
                new ArrayList<>(), new ArrayList<>(), new HashMap<>());

        System.out.println("will check the token for resource=file2 and operation=write");

        SymbolTable check_symbols2 = new SymbolTable(finalBiscuit.symbols);
        List<Fact> ambient_facts2 = List.of(
                fact("resource", List.of(s("file2"))).convert(check_symbols2),
                fact("operation", List.of(s("write"))).convert(check_symbols2)
        );

        try {
            finalBiscuit.check(check_symbols2, ambient_facts2,
                    new ArrayList<>(), new ArrayList<>(), new HashMap<>());
            fail();
        } catch (Error e) {
            System.out.println(e);
            assertEquals(
                    new Error.FailedLogic(new LogicError.Unauthorized(new LogicError.MatchedPolicy.Allow(0), List.of(
                            new FailedCheck.FailedBlock(1, 0, "check if resource($resource), operation(\"read\"), right($resource, \"read\")"),
                            new FailedCheck.FailedBlock(2, 0, "check if resource(\"file1\")")
                    ))),
                    e);
        }
    }
}