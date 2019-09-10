package com.clevercloud.biscuit.serialization;

import cafe.cryptography.curve25519.InvalidEncodingException;
import com.clevercloud.biscuit.crypto.KeyPair;
import com.clevercloud.biscuit.datalog.*;
import com.clevercloud.biscuit.token.Biscuit;
import com.clevercloud.biscuit.token.Block;
import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;

import static com.clevercloud.biscuit.crypto.TokenSignature.hex;
import static io.vavr.API.Right;

public class BiscuitTest extends TestCase {
    public BiscuitTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(BiscuitTest.class);
    }

    public void testSerialize() throws IOException, InvalidEncodingException {
        byte[] seed = {0, 0, 0, 0};
        SecureRandom rng = new SecureRandom(seed);

        System.out.println("preparing the authority block");

        KeyPair root = new KeyPair(rng);

        SymbolTable symbols = Biscuit.default_symbol_table();
        int symbol_start = symbols.symbols.size();
        ID authority = symbols.add("authority");
        long right = symbols.insert("right");
        ID file1 = symbols.add("file1");
        ID file2 = symbols.add("file2");
        ID read  = symbols.add("read");
        ID write = symbols.add("write");

        ArrayList<Fact> facts = new ArrayList<>();
        ArrayList<ID> ids = new ArrayList<>();
        ids.add(authority);
        ids.add(file1);
        ids.add(read);
        facts.add(new Fact(new Predicate(right, ids)));
        ids = new ArrayList<>();
        ids.add(authority);
        ids.add(file2);
        ids.add(read);
        facts.add(new Fact(new Predicate(right, ids)));
        ids = new ArrayList<>();
        ids.add(authority);
        ids.add(file1);
        ids.add(write);
        facts.add(new Fact(new Predicate(right, ids)));

        SymbolTable block_symbols = new SymbolTable();
        for(int i = symbol_start; i < symbols.symbols.size(); i++) {
            block_symbols.add(symbols.symbols.get(i));
        }

        Block authority_block = new Block(0, block_symbols, facts, new ArrayList<>());

        System.out.println(authority_block.print(symbols));

        Biscuit b = Biscuit.make(rng, root, authority_block).get();

        System.out.println(b.print());

        System.out.println("serializing the first token");

        byte[] data = b.serialize().get();

        System.out.print("data len: ");
        System.out.println(data.length);
        System.out.println(hex(data));

        System.out.println("deserializing the first token");
        Biscuit deser = Biscuit.from_bytes(data, root.public_key).get();

        System.out.println(deser.print());

        // SECOND BLOCK
        System.out.println("preparing the second block");

        KeyPair keypair2 = new KeyPair(rng);
        SymbolTable symbols2 = new SymbolTable(deser.symbols);
        int symbol_start2 = symbols2.symbols.size();
        long caveat1 = symbols2.insert("caveat1");
        long resource = symbols2.insert("resource");
        long operation = symbols2.insert("operation");
        ID ambient = symbols2.add("ambient");
        ID var0 = new ID.Variable(0);

        ArrayList<ID> head_ids = new ArrayList<>();
        head_ids.add(var0);
        Predicate head = new Predicate(caveat1, head_ids);

        ArrayList<Predicate> body = new ArrayList<>();
        ids = new ArrayList<>();
        ids.add(ambient);
        ids.add(var0);
        body.add(new Predicate(resource, ids));
        ids = new ArrayList<>();
        ids.add(ambient);
        ids.add(read);
        body.add(new Predicate(operation, ids));
        ids = new ArrayList<>();
        ids.add(authority);
        ids.add(var0);
        ids.add(read);
        body.add(new Predicate(right, ids));

        ArrayList<Rule> caveats = new ArrayList<>();
        caveats.add(new Rule(head, body, new ArrayList<>()));

        SymbolTable block_symbols2 = new SymbolTable();
        for(int i = symbol_start2; i < symbols2.symbols.size(); i++) {
            block_symbols2.add(symbols2.symbols.get(i));
        }

        Block block2 = new Block(1, block_symbols2, new ArrayList<>(), caveats);

        Biscuit b2 = deser.append(rng, keypair2, block2).get();

        System.out.println(b2.print());

        System.out.println("serializing the second token");

        byte[] data2 = b2.serialize().get();

        System.out.print("data len: ");
        System.out.println(data2.length);
        System.out.println(hex(data2));

        System.out.println("deserializing the second token");
        Biscuit deser2 = Biscuit.from_bytes(data2, root.public_key).get();

        System.out.println(deser2.print());

        /*
        String message1 = "hello";
        KeyPair keypair1 = new KeyPair(rng);
        Token token1 = new Token(rng, keypair1, message1.getBytes());
        Assert.assertEquals(Right(null), token1.verify());

        Schema.Biscuit biscuitSer = token1.serialize();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        biscuitSer.writeTo(stream);
        byte[] data = stream.toByteArray();

        Schema.Biscuit biscuitDeser = Schema.Biscuit.parseFrom(data);

        Token tokenDeser = Token.deserialize(biscuitDeser);
        Assert.assertEquals(Right(null), tokenDeser.verify());
        System.out.println("token1");
        for(byte[] block: token1.blocks) {
            System.out.println(hex(block));
        }
        System.out.println("tokenDeser");
        for(byte[] block: tokenDeser.blocks) {
            System.out.println(hex(block));
        }
        Assert.assertEquals(token1.blocks, tokenDeser.blocks);
        */
    }
}
