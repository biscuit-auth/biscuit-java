package com.clevercloud.biscuit.token;

import cafe.cryptography.curve25519.CompressedRistretto;
import cafe.cryptography.curve25519.InvalidEncodingException;
import cafe.cryptography.curve25519.RistrettoElement;
import com.clevercloud.biscuit.error.LogicError;
import io.vavr.control.Either;
import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import sun.misc.IOUtils;

import java.io.IOException;
import java.io.InputStream;

import static com.clevercloud.biscuit.crypto.TokenSignature.fromHex;
import static com.clevercloud.biscuit.crypto.TokenSignature.hex;

public class SamplesTest extends TestCase {

    public SamplesTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(SamplesTest.class);
    }

    public void testBasic() throws IOException, InvalidEncodingException {
        byte[] rootData = fromHex("da905388864659eb785877a319fbc42c48e2f8a40af0c5baea0ef8ff7c795253");
        RistrettoElement root = (new CompressedRistretto(rootData)).decompress();

        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("test1_basic.bc");

        System.out.println("a");
        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);

        Biscuit token = Biscuit.from_bytes(data, root).get();
        System.out.println(token.print());

        Verifier v1 = new Verifier();
        v1.resource("file1");
        v1.operation("read");
        Either<LogicError, Void> res = v1.verify(token);
        if(res.isLeft()) {
            System.out.println("error: "+res.getLeft());
        }
        Assert.assertTrue(res.isRight());

        byte[] serialized = token.serialize().get();
        Assert.assertEquals(data.length, serialized.length);
        System.out.println(hex(data));
        System.out.println(hex(serialized));

        for(int i = 0; i < data.length; i++) {
            Assert.assertEquals(data[i], serialized[i]);
        }
    }
}
