package com.clevercloud.biscuit.token;

import com.clevercloud.biscuit.crypto.PublicKey;
import com.clevercloud.biscuit.datalog.RunLimits;
import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.datalog.World;
import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.error.FailedCaveat;
import com.clevercloud.biscuit.error.LogicError;
import com.clevercloud.biscuit.token.builder.*;
import io.vavr.control.Either;
import io.vavr.control.Option;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.clevercloud.biscuit.token.builder.Utils.*;
import static io.vavr.API.Left;
import static io.vavr.API.Right;

/**
 * Token verification class
 */
public class Verifier {
    Biscuit token;
    List<Caveat> caveats;
    World base_world;
    World world;
    SymbolTable base_symbols;
    SymbolTable symbols;

    private Verifier(Biscuit token, World w) {
        this.token = token;
        this.base_world = w;
        this.world = new World(this.base_world);
        this.base_symbols = new SymbolTable(this.token.symbols);
        this.symbols = new SymbolTable(this.token.symbols);
        this.caveats = new ArrayList<>();
    }

    /**
     * Creates a verifier for a token
     *
     * also checks that the token is valid for this root public key
     * @param token
     * @param root
     * @return
     */
    static public Either<Error, Verifier> make(Biscuit token, Option<PublicKey> root) {
        if(!token.is_sealed()) {
            Either<Error, Void> res = token.check_root_key(root.get());
            if (res.isLeft()) {
                Error e = res.getLeft();
                return Left(e);
            }
        }

        Either<Error, World> res = token.generate_world();
        if (res.isLeft()) {
            Error e = res.getLeft();
            System.out.println(e);
            return Left(e);
        }

        return Right(new Verifier(token, res.get()));
    }

    public void reset() {
        this.world = new World(this.base_world);
        this.symbols = new SymbolTable(this.base_symbols);
        this.caveats = new ArrayList<>();
    }

    public void snapshot() {
        this.base_world = new World(this.world);
        this.base_symbols = new SymbolTable(this.symbols);
    }

    public void add_fact(Fact fact) {
        world.add_fact(fact.convert(symbols));
    }

    public void add_rule(Rule rule) {
        world.add_rule(rule.convert(symbols));
    }

    public void add_caveat(Caveat caveat) {
        this.caveats.add(caveat);
        world.add_caveat(caveat.convert(symbols));
    }

    public void add_resource(String resource) {
        world.add_fact(fact("resource", Arrays.asList(s("ambient"), string(resource))).convert(symbols));
    }

    public void add_operation(String operation) {
        world.add_fact(fact("operation", Arrays.asList(s("ambient"), s(operation))).convert(symbols));
    }

    public void set_time() {

        world.add_fact(fact("time", Arrays.asList(s("ambient"), date(new Date()))).convert(symbols));
    }

    public void revocation_check(List<Long> ids) {
        ArrayList<Rule> q = new ArrayList<>();

        q.add(constrained_rule(
                "revocation_check",
                Arrays.asList((var("id"))),
                Arrays.asList(pred("revocation_id", Arrays.asList(var("id")))),
                Arrays.asList(new Expression.Binary(Expression.Op.NotIn, new Expression.Value(var("id")),
                        new Expression.Value(new Term.Set(new HashSet(ids)))))
        ));

        this.caveats.add(new Caveat(q));
    }

    public Either<Error, List<String>> get_revocation_ids() {
        ArrayList<String> ids = new ArrayList<>();

        final Rule getRevocationIds = rule(
                "revocation_id",
                Arrays.asList(var("id")),
                Arrays.asList(pred("revocation_id", Arrays.asList(var("id"))))
        );

        Either<Error, Set<Fact>> queryRes = this.query(getRevocationIds);
        if (queryRes.isLeft()) {
            Error e = queryRes.getLeft();
            System.out.println(e);
            return Left(e);
        }

        queryRes.get().stream().forEach(fact -> {
            fact.ids().stream().forEach(id -> {
                if (id instanceof Term.Str) {
                    ids.add((((Term.Str) id).value()));
                }
            });
        });

        return Right(ids);
    }

    public Either<Error, Set<Fact>> query(Rule query) {
        return this.query(query, new RunLimits());
    }

    public Either<Error, Set<Fact>> query(Rule query, RunLimits limits) {
        Either<Error, Void> runRes = world.run(limits);
        if (runRes.isLeft()) {
            Error e = runRes.getLeft();
            System.out.println(e);
            return Left(e);
        }

        Set<com.clevercloud.biscuit.datalog.Fact> facts = world.query_rule(query.convert(symbols));
        Set<Fact> s = new HashSet();

        for(com.clevercloud.biscuit.datalog.Fact f: facts) {
            s.add(Fact.convert_from(f, symbols));
        }

        return Right(s);
    }

    public Either<Error, Void> verify() {
        return this.verify(new RunLimits());
    }

    public Either<Error, Void> verify(RunLimits limits) {
        Instant timeLimit = Instant.now().plus(limits.maxTime);

        if(this.symbols.get("authority").isEmpty() || this.symbols.get("ambient").isEmpty()) {
            return Left(new Error.MissingSymbols());
        }

        Either<Error, Void> runRes = world.run(limits);
        if (runRes.isLeft()) {
            Error e = runRes.getLeft();
            System.out.println(e);
            return Left(e);
        }

        SymbolTable symbols = new SymbolTable(this.symbols);

        ArrayList<FailedCaveat> errors = new ArrayList<>();
        for (int j = 0; j < this.token.authority.caveats.size(); j++) {
            boolean successful = false;
            com.clevercloud.biscuit.datalog.Caveat c = this.token.authority.caveats.get(j);

            for(int k = 0; k < c.queries().size(); k++) {
                Set<com.clevercloud.biscuit.datalog.Fact> res = world.query_rule(c.queries().get(k));

                if(Instant.now().compareTo(timeLimit) >= 0) {
                    return Left(new Error.Timeout());
                }

                if (!res.isEmpty()) {
                    successful = true;
                    break;
                }
            }

            if (!successful) {
                errors.add(new FailedCaveat.FailedBlock(0, j, symbols.print_caveat(this.token.authority.caveats.get(j))));
            }
        }

        for (int j = 0; j < this.caveats.size(); j++) {
            com.clevercloud.biscuit.datalog.Caveat c = this.caveats.get(j).convert(symbols);
            boolean successful = false;

            for(int k = 0; k < c.queries().size(); k++) {
                Set<com.clevercloud.biscuit.datalog.Fact> res = world.query_rule(c.queries().get(k));

                if(Instant.now().compareTo(timeLimit) >= 0) {
                    return Left(new Error.Timeout());
                }

                if (!res.isEmpty()) {
                    successful = true;
                    break;
                }
            }

            if (!successful) {
                errors.add(new FailedCaveat.FailedVerifier(j, symbols.print_caveat(c)));
            }
        }

        for(int i = 0; i < this.token.blocks.size(); i++) {
            Block b = this.token.blocks.get(i);

            for (int j = 0; j < b.caveats.size(); j++) {
                boolean successful = false;
                com.clevercloud.biscuit.datalog.Caveat c = b.caveats.get(j);

                for(int k = 0; k < c.queries().size(); k++) {
                    Set<com.clevercloud.biscuit.datalog.Fact> res = world.query_rule(c.queries().get(k));

                    if(Instant.now().compareTo(timeLimit) >= 0) {
                        return Left(new Error.Timeout());
                    }

                    if (!res.isEmpty()) {
                        successful = true;
                        break;
                    }
                }

                if (!successful) {
                    errors.add(new FailedCaveat.FailedBlock(b.index, j, symbols.print_caveat(b.caveats.get(j))));
                }
            }
        }

        if(errors.isEmpty()) {
            return Right(null);
        } else {
            System.out.println(errors);
            return Left(new Error.FailedLogic(new LogicError.FailedCaveats(errors)));
        }
    }

    public String print_world() {
        final List<String> facts = this.world.facts().stream().map((f) -> this.symbols.print_fact(f)).collect(Collectors.toList());
        final List<String> rules = this.world.rules().stream().map((r) -> this.symbols.print_rule(r)).collect(Collectors.toList());

        List<String> caveats = new ArrayList<>();

        for (int j = 0; j < this.caveats.size(); j++) {
            caveats.add("Verifier["+j+"]: "+this.caveats.get(j).toString());
        }

        for (int j = 0; j < this.token.authority.caveats.size(); j++) {
            caveats.add("Block[0]["+j+"]: "+this.symbols.print_caveat(this.token.authority.caveats.get(j)));
        }

        for(int i = 0; i < this.token.blocks.size(); i++) {
            Block b = this.token.blocks.get(i);

            for (int j = 0; j < b.caveats.size(); j++) {
                caveats.add("Block["+i+"]["+j+"]: "+this.symbols.print_caveat(b.caveats.get(j)));
            }
        }

        StringBuilder b = new StringBuilder();
        b.append("World {\n\tfacts: [\n\t\t");
        b.append(String.join(",\n\t\t", facts));
        b.append("\n\t],\n\trules: [\n\t\t");
        b.append(String.join(",\n\t\t", rules));
        b.append("\n\t],\n\tcaveats: [\n\t\t");
        b.append(String.join(",\n\t\t", caveats));
        b.append("\n\t]\n}");

        return b.toString();
    }
}
