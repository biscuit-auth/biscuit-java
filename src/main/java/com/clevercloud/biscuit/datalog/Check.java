package com.clevercloud.biscuit.datalog;

import biscuit.format.schema.Schema;
import com.clevercloud.biscuit.error.Error;
import io.vavr.control.Either;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.vavr.API.Left;
import static io.vavr.API.Right;

public class Check {
    private final List<Rule> queries;

    public Check(List<Rule> queries) {
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

    public Schema.CheckV2 serialize() {
        Schema.CheckV2.Builder b = Schema.CheckV2.newBuilder();

        for(int i = 0; i < this.queries.size(); i++) {
            b.addQueries(this.queries.get(i).serialize());
        }

        return b.build();
    }

    static public Either<Error.FormatError, Check> deserializeV2(Schema.CheckV2 check) {
        ArrayList<Rule> queries = new ArrayList<>();

        for (Schema.RuleV2 query: check.getQueriesList()) {
            Either<Error.FormatError, Rule> res = Rule.deserializeV2(query);
            if(res.isLeft()) {
                Error.FormatError e = res.getLeft();
                return Left(e);
            } else {
                queries.add(res.get());
            }
        }

        return Right(new Check(queries));
    }
}
