package com.clevercloud.biscuit.datalog;

import biscuit.format.schema.Schema;
import com.clevercloud.biscuit.datalog.constraints.Constraint;
import com.clevercloud.biscuit.error.Error;
import io.vavr.control.Either;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

public class Caveat {
    private final List<Rule> queries;

    public Caveat(List<Rule> queries) {
        this.queries = queries;
    }

    public List<Rule> queries() {
        return queries;
    }

    @Override
    public int hashCode() {
        return Objects.hash(queries);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public Schema.CaveatV1 serialize() {
        Schema.CaveatV1.Builder b = Schema.CaveatV1.newBuilder();

        for(int i = 0; i < this.queries.size(); i++) {
            b.addQueries(this.queries.get(i).serialize());
        }

        return b.build();
    }

    static public Either<Error.FormatError, Caveat> deserializeV0(Schema.CaveatV0 caveat) {
        ArrayList<Rule> queries = new ArrayList<>();

        for (Schema.RuleV0 query: caveat.getQueriesList()) {
            Either<Error.FormatError, Rule> res = Rule.deserializeV0(query);
            if(res.isLeft()) {
                Error.FormatError e = res.getLeft();
                return Left(e);
            } else {
                queries.add(res.get());
            }
        }

        return Right(new Caveat(queries));
    }

    static public Either<Error.FormatError, Caveat> deserializeV1(Schema.CaveatV1 caveat) {
        ArrayList<Rule> queries = new ArrayList<>();

        for (Schema.RuleV1 query: caveat.getQueriesList()) {
            Either<Error.FormatError, Rule> res = Rule.deserializeV1(query);
            if(res.isLeft()) {
                Error.FormatError e = res.getLeft();
                return Left(e);
            } else {
                queries.add(res.get());
            }
        }

        return Right(new Caveat(queries));
    }
}
