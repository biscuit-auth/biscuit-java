package com.clevercloud.biscuit.serialization;

import biscuit.format.schema.Schema;
import cafe.cryptography.curve25519.InvalidEncodingException;
import com.clevercloud.biscuit.crypto.KeyPair;
import com.clevercloud.biscuit.crypto.SignatureTest;
import com.clevercloud.biscuit.crypto.Token;
import com.clevercloud.biscuit.datalog.Fact;
import com.clevercloud.biscuit.datalog.ID;
import com.clevercloud.biscuit.datalog.Predicate;
import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.token.Biscuit;
import com.clevercloud.biscuit.token.Block;
import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.ByteArrayOutputStream;
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
        KeyPair root = new KeyPair(rng);

        SymbolTable symbols = Biscuit.default_symbol_table();
        int symbol_start = symbols.symbols.size();
        ID authority = symbols.add("authority");
        ID right = symbols.add("right");
        ID file1 = symbols.add("file1");
        ID file2 = symbols.add("file2");
        ID read  = symbols.add("read");
        ID write = symbols.add("write");

        ArrayList<Fact> facts = new ArrayList<>();
        ArrayList<ID> ids = new ArrayList<>();
        ids.add(authority);
        ids.add(file1);
        ids.add(read);
        facts.add(new Fact(new Predicate(symbols.insert("right"), ids)));
        ids = new ArrayList<>();
        ids.add(authority);
        ids.add(file2);
        ids.add(read);
        facts.add(new Fact(new Predicate(symbols.insert("right"), ids)));
        ids = new ArrayList<>();
        ids.add(authority);
        ids.add(file1);
        ids.add(write);
        facts.add(new Fact(new Predicate(symbols.insert("right"), ids)));

        SymbolTable block_symbols = new SymbolTable();
        for(int i = symbol_start; i < symbols.symbols.size(); i++) {
            block_symbols.add(symbols.symbols.get(i));
        }


        Block authority_block = new Block(0, block_symbols, facts, new ArrayList<>());

        System.out.println(authority_block.print(symbols));

        Biscuit b = Biscuit.make(rng, root, authority_block).get();

        System.out.println(b.print());

        byte[] data = b.serialize().get();

        System.out.print("data len: ");
        System.out.println(data.length);
        System.out.println(bytesToHex(data));

        Biscuit deser = Biscuit.from_bytes(data, root.public_key).get();

        System.out.println(deser.print());
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

    static String bytesToHex(byte[] hashInBytes) {

        StringBuilder sb = new StringBuilder();
        for (byte b : hashInBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();

    }
}
