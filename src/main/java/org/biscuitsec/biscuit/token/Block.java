package org.biscuitsec.biscuit.token;

import biscuit.format.schema.Schema;
import org.biscuitsec.biscuit.crypto.PublicKey;
import org.biscuitsec.biscuit.error.Error;
import org.biscuitsec.biscuit.datalog.*;
import org.biscuitsec.biscuit.token.format.SerializedBiscuit;
import com.google.protobuf.InvalidProtocolBufferException;
import io.vavr.control.Either;
import io.vavr.control.Option;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

/**
 * Represents a token's block with its checks
 */
public class Block {
    final SymbolTable symbols;
    final String context;
    final List<Fact> facts;
    final List<Rule> rules;
    final List<Check> checks;
    final List<Scope> scopes;
    final List<PublicKey> publicKeys;
    final Option<PublicKey> externalKey;
    final long version;

    /**
     * creates a new block
     *
     * @param base_symbols
     */
    public Block(SymbolTable base_symbols) {
        this.symbols = base_symbols;
        this.context = "";
        this.facts = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.checks = new ArrayList<>();
        this.scopes = new ArrayList<>();
        this.publicKeys = new ArrayList<>();
        this.externalKey = Option.none();
        this.version = SerializedBiscuit.MAX_SCHEMA_VERSION;
    }

    /**
     * creates a new block
     *
     * @param base_symbols
     * @param facts
     * @param checks
     */
    public Block(SymbolTable base_symbols, String context, List<Fact> facts, List<Rule> rules, List<Check> checks,
                 List<Scope> scopes, List<PublicKey> publicKeys, Option<PublicKey> externalKey, int version) {
        this.symbols = base_symbols;
        this.context = context;
        this.facts = facts;
        this.rules = rules;
        this.checks = checks;
        this.scopes = scopes;
        this.version = version;
        this.publicKeys = publicKeys;
        this.externalKey = externalKey;
    }

    public SymbolTable symbols() {
        return symbols;
    }

    public List<PublicKey> publicKeys() {
        return publicKeys;
    }

    /**
     * pretty printing for a block
     *
     * @param symbol_table
     * @return
     */
    public String print(SymbolTable symbol_table) {
        StringBuilder s = new StringBuilder();

        s.append("Block");
        s.append(" {\n\t\tsymbols: ");
        s.append(this.symbols.symbols);
        s.append("\n\t\tcontext: ");
        s.append(this.context);
        if(this.externalKey.isDefined()) {
            s.append("\n\t\texternal key: ");
            s.append(this.externalKey.get().toString());
        }
        s.append("\n\t\tscopes: [");
        for (Scope scope : this.scopes) {
            s.append("\n\t\t\t");
            s.append(symbol_table.print_scope(scope));
        }
        s.append("\n\t\t]\n\t\tfacts: [");
        for (Fact f : this.facts) {
            s.append("\n\t\t\t");
            s.append(symbol_table.print_fact(f));
        }
        s.append("\n\t\t]\n\t\trules: [");
        for (Rule r : this.rules) {
            s.append("\n\t\t\t");
            s.append(symbol_table.print_rule(r));
        }
        s.append("\n\t\t]\n\t\tchecks: [");
        for (Check c : this.checks) {
            s.append("\n\t\t\t");
            s.append(symbol_table.print_check(c));
        }
        s.append("\n\t\t]\n\t}");

        return s.toString();
    }

    /**
     * Serializes a Block to its Protobuf representation
     *
     * @return
     */
    public Schema.Block serialize() {
        Schema.Block.Builder b = Schema.Block.newBuilder();

        for (int i = 0; i < this.symbols.symbols.size(); i++) {
            b.addSymbols(this.symbols.symbols.get(i));
        }

        if (!this.context.isEmpty()) {
            b.setContext(this.context);
        }

        for (int i = 0; i < this.facts.size(); i++) {
            b.addFactsV2(this.facts.get(i).serialize());
        }

        for (int i = 0; i < this.rules.size(); i++) {
            b.addRulesV2(this.rules.get(i).serialize());
        }

        for (int i = 0; i < this.checks.size(); i++) {
            b.addChecksV2(this.checks.get(i).serialize());
        }

        for (Scope scope: this.scopes) {
            b.addScope(scope.serialize());
        }

        for(PublicKey pk: this.publicKeys) {
            b.addPublicKeys(pk.serialize());
        }

        b.setVersion(SerializedBiscuit.MAX_SCHEMA_VERSION);
        return b.build();
    }

    /**
     * Deserializes a block from its Protobuf representation
     *
     * @param b
     * @return
     */
    static public Either<Error.FormatError, Block> deserialize(Schema.Block b, Option<PublicKey> externalKey) {
        int version = b.getVersion();
        if (version < SerializedBiscuit.MIN_SCHEMA_VERSION || version > SerializedBiscuit.MAX_SCHEMA_VERSION) {
            return Left(new Error.FormatError.Version(SerializedBiscuit.MIN_SCHEMA_VERSION, SerializedBiscuit.MAX_SCHEMA_VERSION, version));
        }

        SymbolTable symbols = new SymbolTable();
        for (String s : b.getSymbolsList()) {
            symbols.add(s);
        }

        ArrayList<Fact> facts = new ArrayList<>();
        ArrayList<Rule> rules = new ArrayList<>();
        ArrayList<Check> checks = new ArrayList<>();

        for (Schema.FactV2 fact : b.getFactsV2List()) {
            Either<Error.FormatError, Fact> res = Fact.deserializeV2(fact);
            if (res.isLeft()) {
                Error.FormatError e = res.getLeft();
                return Left(e);
            } else {
                facts.add(res.get());
            }
        }


        for (Schema.RuleV2 rule : b.getRulesV2List()) {
            Either<Error.FormatError, Rule> res = Rule.deserializeV2(rule);
            if (res.isLeft()) {
                Error.FormatError e = res.getLeft();
                return Left(e);
            } else {
                rules.add(res.get());
            }
        }


        for (Schema.CheckV2 check : b.getChecksV2List()) {
            Either<Error.FormatError, Check> res = Check.deserializeV2(check);
            if (res.isLeft()) {
                Error.FormatError e = res.getLeft();
                return Left(e);
            } else {
                checks.add(res.get());
            }
        }

        ArrayList<Scope> scopes = new ArrayList<>();
        for (Schema.Scope scope: b.getScopeList()) {
            Either<Error.FormatError, Scope> res = Scope.deserialize(scope);
            if(res.isLeft()) {
                Error.FormatError e = res.getLeft();
                return Left(e);
            } else {
                scopes.add(res.get());
            }
        }

        ArrayList<PublicKey> publicKeys = new ArrayList<>();
        for (Schema.PublicKey pk: b.getPublicKeysList()) {
            try {
                publicKeys.add(PublicKey.deserialize(pk));
            } catch(Error.FormatError e) {
                return Left(e);
            }
        }

        SchemaVersion schemaVersion = new SchemaVersion(facts, rules, checks, scopes);
        Either<Error.FormatError, Void> res = schemaVersion.checkCompatibility(version);
        if (res.isLeft()) {
            Error.FormatError e = res.getLeft();
            return Left(e);
        }

        return Right(new Block(symbols, b.getContext(), facts, rules, checks, scopes, publicKeys, externalKey, version));
    }

    /**
     * Deserializes a Block from a byte array
     *
     * @param slice
     * @return
     */
    static public Either<Error.FormatError, Block> from_bytes(byte[] slice, Option<PublicKey> externalKey) {
        try {
            Schema.Block data = Schema.Block.parseFrom(slice);
            return Block.deserialize(data, externalKey);
        } catch (InvalidProtocolBufferException e) {
            return Left(new Error.FormatError.DeserializationError(e.toString()));
        }
    }

    public Either<Error.FormatError, byte[]> to_bytes() {
        Schema.Block b = this.serialize();
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            b.writeTo(stream);
            byte[] data = stream.toByteArray();
            return Right(data);
        } catch (IOException e) {
            return Left(new Error.FormatError.SerializationError(e.toString()));
        }
    }
}

