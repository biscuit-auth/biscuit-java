package com.clevercloud.biscuit.token;

import biscuit.format.schema.Schema;
import com.clevercloud.biscuit.crypto.PublicKey;
import com.clevercloud.biscuit.datalog.ID;
import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.datalog.World;
import com.clevercloud.biscuit.datalog.constraints.Constraint;
import com.clevercloud.biscuit.datalog.constraints.ConstraintKind;
import com.clevercloud.biscuit.datalog.constraints.IntConstraint;
import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.error.FailedCaveat;
import com.clevercloud.biscuit.error.LogicError;
import com.clevercloud.biscuit.token.builder.Atom;
import com.clevercloud.biscuit.token.builder.Fact;
import com.clevercloud.biscuit.token.builder.Rule;
import com.clevercloud.biscuit.token.builder.Caveat;
import io.vavr.control.Either;
import io.vavr.control.Option;

import java.util.*;

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
                Arrays.asList((var(0))),
                Arrays.asList(pred("revocation_id", Arrays.asList(var(0)))),
                Arrays.asList(new com.clevercloud.biscuit.token.builder.constraints.IntConstraint.NotInSet(0, new HashSet(ids)))
        ));

        this.caveats.add(new Caveat(q));
    }

    public List<UUID> get_revocation_ids() {
        ArrayList<UUID> ids = new ArrayList<>();

        final Rule getRevocationIds = rule(
                "revocation_id",
                Arrays.asList(var(0)),
                Arrays.asList(pred("revocation_id", Arrays.asList(var(0))))
        );

        this.query(getRevocationIds).parallelStream().forEach(fact -> {
            fact.ids().parallelStream().forEach(id -> {
                if (id instanceof Atom.Str) {
                    ids.add(UUID.fromString((((Atom.Str) id).value())));
                }
            });
        });

        return ids;
    }

    public Set<Fact> query(Rule query) {
        world.run();
        Set<com.clevercloud.biscuit.datalog.Fact> facts = world.query_rule(query.convert(symbols));
        Set<Fact> s = new HashSet();

        for(com.clevercloud.biscuit.datalog.Fact f: facts) {
            s.add(Fact.convert_from(f, symbols));
        }

        return s;
    }

    public Either<Error, Void> verify() {
        if(this.symbols.get("authority").isEmpty() || this.symbols.get("ambient").isEmpty()) {
            return Left(new Error().new MissingSymbols());
        }

        world.run();
        SymbolTable symbols = new SymbolTable(this.symbols);

        ArrayList<FailedCaveat> errors = new ArrayList<>();
        for (int j = 0; j < this.token.authority.caveats.size(); j++) {
            boolean successful = false;
            com.clevercloud.biscuit.datalog.Caveat c = this.token.authority.caveats.get(j);

            for(int k = 0; k < c.queries().size(); k++) {
                Set<com.clevercloud.biscuit.datalog.Fact> res = world.query_rule(c.queries().get(k));
                if (!res.isEmpty()) {
                    successful = true;
                    break;
                }
            }

            if (!successful) {
                errors.add(new FailedCaveat().
                        new FailedBlock(0, j, symbols.print_caveat(this.token.authority.caveats.get(j))));
            }
        }

        for (int j = 0; j < this.caveats.size(); j++) {
            com.clevercloud.biscuit.datalog.Caveat c = this.caveats.get(j).convert(symbols);
            boolean successful = false;

            for(int k = 0; k < c.queries().size(); k++) {
                Set<com.clevercloud.biscuit.datalog.Fact> res = world.query_rule(c.queries().get(k));
                if (!res.isEmpty()) {
                    successful = true;
                    break;
                }
            }

            if (!successful) {
                errors.add(new FailedCaveat().
                        new FailedVerifier(j, symbols.print_caveat(c)));
            }
        }

        for(int i = 0; i < this.token.blocks.size(); i++) {
            Block b = this.token.blocks.get(i);

            for (int j = 0; j < b.caveats.size(); j++) {
                boolean successful = false;
                com.clevercloud.biscuit.datalog.Caveat c = b.caveats.get(j);

                for(int k = 0; k < c.queries().size(); k++) {
                    Set<com.clevercloud.biscuit.datalog.Fact> res = world.query_rule(c.queries().get(k));
                    if (!res.isEmpty()) {
                        successful = true;
                        break;
                    }
                }

                if (!successful) {
                    errors.add(new FailedCaveat().
                            new FailedBlock(b.index, j, symbols.print_caveat(b.caveats.get(j))));
                }
            }
        }

        if(errors.isEmpty()) {
            return Right(null);
        } else {
            System.out.println(errors);
            return Left(new Error().new FailedLogic(new LogicError().new FailedCaveats(errors)));
        }
    }

    public String print_world() {
        return this.symbols.print_world(this.world);
    }
}
