package com.clevercloud.biscuit.token;

import biscuit.format.schema.Schema;
import com.clevercloud.biscuit.crypto.KeyPair;
import com.clevercloud.biscuit.crypto.PublicKey;
import com.clevercloud.biscuit.datalog.AuthorizedWorld;
import com.clevercloud.biscuit.datalog.RunLimits;
import com.clevercloud.biscuit.error.Error;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Try;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class SamplesJsonV2Test {
    final RunLimits runLimits = new RunLimits(500,100, Duration.ofMillis(500));
    @TestFactory
    Stream<DynamicTest> jsonTest() {
        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("v2/samples.json");
        Gson gson = new Gson();
        Sample sample = gson.fromJson(new InputStreamReader(new BufferedInputStream(inputStream)), Sample.class);
        PublicKey publicKey = new PublicKey(Schema.PublicKey.Algorithm.Ed25519, sample.root_public_key);
        KeyPair keyPair = new KeyPair(sample.root_private_key);
        return sample.testcases.stream().map(t -> process_testcase(t, publicKey, keyPair));
    }

    DynamicTest process_testcase(final TestCase testCase, final PublicKey publicKey, final KeyPair privateKey) {
        return DynamicTest.dynamicTest(testCase.title + ": "+testCase.filename, () -> {
            System.out.println("Testcase name: \""+testCase.title+"\"");
            System.out.println("filename: \""+testCase.filename+"\"");
            InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("v2/" + testCase.filename);
            JsonObject validation = testCase.validations.getAsJsonObject().entrySet().iterator().next().getValue().getAsJsonObject();
            World world = new Gson().fromJson(validation, World.class);
            JsonObject expected_result = validation.getAsJsonObject("result");
            String[] authorizer_facts = validation.getAsJsonPrimitive("authorizer_code").getAsString().split(";");
            Either<Throwable, Tuple2<Long, AuthorizedWorld>> res = Try.of(() -> {
                byte[] data = new byte[inputStream.available()];
                inputStream.read(data);
                Biscuit token = Biscuit.from_bytes(data, publicKey);

                // TODO Add check of the token

                Authorizer authorizer = token.authorizer();
                System.out.println(token.print());
                for (String f : authorizer_facts) {
                    f = f.trim();
                    if (f.length() > 0) {
                        if (f.startsWith("check if")) {
                            authorizer.add_check(f);
                        } else if (f.startsWith("revocation_id")) {
                            // do nothing
                        } else {
                            authorizer.add_fact(f);
                        }
                    }
                }
                authorizer.allow(); // TODO manage the policies
                System.out.println(authorizer.print_world());
                return authorizer.authorize(runLimits);
            }).toEither();
            if (res.isLeft()) {
                Error e = (Error) res.getLeft();
                System.out.println("got error: " + e);
                JsonElement err_json = e.toJson();
                assertEquals(expected_result.get("Err"),err_json);
            } else {
                assertEquals(expected_result.getAsJsonPrimitive("Ok").getAsLong(), res.get()._1);
            }
        });
    }


    class Block {
        List<String> symbols;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        String code;

        public List<String> getSymbols() {
            return symbols;
        }

        public void setSymbols(List<String> symbols) {
            this.symbols = symbols;
        }
    }

    class Token {
        List<Block> blocks;

        public List<Block> getBlocks() {
            return blocks;
        }

        public void setBlocks(List<Block> blocks) {
            this.blocks = blocks;
        }
    }

    class TestCase {
        String title;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        String filename;
        List<Token> tokens;
        JsonElement validations;

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public List<Token> getTokens() {
            return tokens;
        }

        public void setTokens(List<Token> tokens) {
            this.tokens = tokens;
        }

        public JsonElement getValidations() {
            return validations;
        }

        public void setValidations(JsonElement validations) {
            this.validations = validations;
        }
    }

    class Sample {
        String root_private_key;

        public String getRoot_public_key() {
            return root_public_key;
        }

        public void setRoot_public_key(String root_public_key) {
            this.root_public_key = root_public_key;
        }

        String root_public_key;
        List<TestCase> testcases;

        public String getRoot_private_key() {
            return root_private_key;
        }

        public void setRoot_private_key(String root_private_key) {
            this.root_private_key = root_private_key;
        }

        public List<TestCase> getTestcases() {
            return testcases;
        }

        public void setTestcases(List<TestCase> testcases) {
            this.testcases = testcases;
        }
    }

    class World {
        List<String> facts;
        List<String> rules;
        List<String> checks;
        List<String> policies;
    }
}
