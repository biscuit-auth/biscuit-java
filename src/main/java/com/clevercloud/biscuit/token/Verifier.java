package com.clevercloud.biscuit.token;

import com.clevercloud.biscuit.crypto.PublicKey;
import com.clevercloud.biscuit.datalog.SymbolTable;
import com.clevercloud.biscuit.datalog.World;
import com.clevercloud.biscuit.datalog.constraints.Constraint;
import com.clevercloud.biscuit.datalog.constraints.ConstraintKind;
import com.clevercloud.biscuit.datalog.constraints.IntConstraint;
import com.clevercloud.biscuit.error.Error;
import com.clevercloud.biscuit.error.FailedCaveat;
import com.clevercloud.biscuit.error.LogicError;
import com.clevercloud.biscuit.token.builder.Fact;
import com.clevercloud.biscuit.token.builder.Rule;
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
    List<Rule> caveats;
    World base_world;
    World world;
    SymbolTable symbols;

    private Verifier(Biscuit token, World w) {
        this.token = token;
        this.base_world = w;
        this.world = new World(this.base_world);
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
        this.symbols = new SymbolTable(this.token.symbols);
        this.caveats = new ArrayList<>();
    }

    public void add_fact(Fact fact) {
        world.add_fact(fact.convert(symbols));
    }

    public void add_rule(Rule rule) {
        world.add_rule(rule.convert(symbols));
    }

    public void add_caveat(Rule caveat) {
        this.caveats.add(caveat);
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
        this.caveats.add(constrained_rule(
                "revocation_check",
                Arrays.asList((var(0))),
                Arrays.asList(pred("revocation_id", Arrays.asList(var(0)))),
                Arrays.asList(new Constraint(0, new ConstraintKind.Int(new IntConstraint.NotInSet(new HashSet(ids)))))
        ));
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
        if(this.token.symbols.get("authority").isEmpty() || this.token.symbols.get("ambient").isEmpty()) {
            return Left(new Error().new MissingSymbols());
        }

        world.run();
        SymbolTable symbols = new SymbolTable(this.token.symbols);

        ArrayList<com.clevercloud.biscuit.datalog.Rule> caveats = new ArrayList<>();
        for(Rule caveat: this.caveats) {
            caveats.add(caveat.convert(symbols));
        }

        ArrayList<FailedCaveat> errors = new ArrayList<>();
        for (int j = 0; j < this.token.authority.caveats.size(); j++) {
            Set<com.clevercloud.biscuit.datalog.Fact> res = world.query_rule(this.token.authority.caveats.get(j));
            if (res.isEmpty()) {
                errors.add(new FailedCaveat().
                        new FailedBlock(0, j, symbols.print_rule(this.token.authority.caveats.get(j))));
            }
        }

        for (int j = 0; j < this.caveats.size(); j++) {
            com.clevercloud.biscuit.datalog.Rule caveat = this.caveats.get(j).convert(symbols);
            Set<com.clevercloud.biscuit.datalog.Fact> res = world.query_rule(caveat);
            if (res.isEmpty()) {
                errors.add(new FailedCaveat().
                        new FailedVerifier(j, symbols.print_rule(caveat)));
            }
        }

        for(int i = 0; i < this.token.blocks.size(); i++) {
            Block b = this.token.blocks.get(i);
            for (int j = 0; j < b.caveats.size(); j++) {
                Set<com.clevercloud.biscuit.datalog.Fact> res = world.query_rule((b.caveats.get(j)));
                if (res.isEmpty()) {
                    errors.add(new FailedCaveat().
                            new FailedBlock(b.index, j, symbols.print_rule(b.caveats.get(j))));
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
}
