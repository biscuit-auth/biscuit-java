package org.biscuitsec.biscuit.datalog;

import io.vavr.control.Option;
import org.biscuitsec.biscuit.crypto.PublicKey;
import org.biscuitsec.biscuit.datalog.expressions.Expression;
import org.biscuitsec.biscuit.token.builder.Utils;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public final class SymbolTable implements Serializable {

    public static final short DEFAULT_SYMBOLS_OFFSET = 1024;
    /**
     * According to <a href="https://github.com/biscuit-auth/biscuit/blob/master/SPECIFICATIONS.md#symbol-table">the specification</a>,
     * We need two symbols tables:
     * * one for the defaults symbols indexed from 0 et 1023 in <code>DEFAULT_SYMBOLS</code> list
     * * one for the usages symbols indexed from 1024 in <code>symbols</code> list
     */
    public static final List<String> DEFAULT_SYMBOLS = List.of(
            "read",
            "write",
            "resource",
            "operation",
            "right",
            "time",
            "role",
            "owner",
            "tenant",
            "namespace",
            "user",
            "team",
            "service",
            "admin",
            "email",
            "group",
            "member",
            "ip_address",
            "client",
            "client_ip",
            "domain",
            "path",
            "version",
            "cluster",
            "node",
            "hostname",
            "nonce",
            "query"
    );

    public final List<String> symbols;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_INSTANT;
    private final List<PublicKey> publicKeys;

    public SymbolTable() {
        this.symbols = new ArrayList<>();
        this.publicKeys = new ArrayList<>();
    }


    public SymbolTable(SymbolTable s) {
        this.symbols = new ArrayList<>();
        symbols.addAll(s.symbols);
        this.publicKeys = new ArrayList<>();
        publicKeys.addAll(s.publicKeys);
    }

    @SuppressWarnings("unused")
    public SymbolTable(List<String> symbols) {
        this.symbols = new ArrayList<>(symbols);
        this.publicKeys = new ArrayList<>();
    }

    public SymbolTable(List<String> symbols, List<PublicKey> publicKeys) {
        this.symbols = new ArrayList<>();
        this.symbols.addAll(symbols);
        this.publicKeys = new ArrayList<>();
        this.publicKeys.addAll(publicKeys);
    }

    public Term add(final String symbol) {
        return new Term.Str(this.insert(symbol));
    }

    public int currentOffset() {
        return this.symbols.size();
    }

    public int currentPublicKeyOffset() {
        return this.publicKeys.size();
    }

    public String fromEpochIsoDate(long epochSec) {
        return Instant.ofEpochSecond(epochSec).atOffset(ZoneOffset.ofTotalSeconds(0)).format(dateTimeFormatter);
    }

    public Option<Long> get(final String symbol) {
        // looking for symbol in default symbols
        long index = DEFAULT_SYMBOLS.indexOf(symbol);
        if (index == -1) {
            // looking for symbol in usages defined symbols
            index = this.symbols.indexOf(symbol);
            if (index == -1) {
                return Option.none();
            } else {
                return Option.some(index + DEFAULT_SYMBOLS_OFFSET);
            }
        } else {
            return Option.some(index);
        }
    }

    public List<String> getAllSymbols() {
        ArrayList<String> allSymbols = new ArrayList<>();
        allSymbols.addAll(DEFAULT_SYMBOLS);
        allSymbols.addAll(symbols);
        return allSymbols;
    }

    public Option<PublicKey> getPk(int i) {
        if (i >= 0 && i < this.publicKeys.size()) {
            return Option.some(this.publicKeys.get(i));
        } else {
            return Option.none();
        }
    }

    public Option<String> getS(int i) {
        if (i >= 0 && i < DEFAULT_SYMBOLS.size() && i < DEFAULT_SYMBOLS_OFFSET) {
            return Option.some(DEFAULT_SYMBOLS.get(i));
        } else if (i >= DEFAULT_SYMBOLS_OFFSET && i < this.symbols.size() + DEFAULT_SYMBOLS_OFFSET) {
            return Option.some(this.symbols.get(i - DEFAULT_SYMBOLS_OFFSET));
        } else {
            return Option.none();
        }
    }

    public long insert(final String symbol) {
        int index = DEFAULT_SYMBOLS.indexOf(symbol);
        if (index == -1) {
            index = this.symbols.indexOf(symbol);
            if (index == -1) {
                this.symbols.add(symbol);
                return this.symbols.size() - 1 + DEFAULT_SYMBOLS_OFFSET;
            } else {
                return index + DEFAULT_SYMBOLS_OFFSET;
            }
        } else {
            return index;
        }
    }

    public long insert(final PublicKey publicKey) {
        int index = this.publicKeys.indexOf(publicKey);
        if (index == -1) {
            this.publicKeys.add(publicKey);
            return this.publicKeys.size() - 1;
        } else {
            return index;
        }
    }

    public String printCheck(final Check c) {
        String prefix;
        switch (c.kind()) {
            case All:
                prefix = "check all ";
                break;
            case One:
            default:
                prefix = "check if ";
                break;
        }
        final List<String> queries = c.queries().stream().map(this::printRuleBody).collect(toList());
        return prefix + String.join(" or ", queries);
    }

    public String printExpression(final Expression e) {
        return e.print(this).get();
    }

    public String printFact(final Fact f) {
        return this.printPredicate(f.predicate());
    }

    public String printPredicate(final Predicate p) {
        List<String> ids = p.terms().stream().map(this::printTerm).collect(toList());
        return Optional.ofNullable(this.printSymbol((int) p.name())).orElse("<?>") + "(" + String.join(", ", ids) + ")";
    }

    public String printRule(final Rule r) {
        String res = this.printPredicate(r.head());
        res += " <- " + this.printRuleBody(r);

        return res;
    }

    public String printRuleBody(final Rule r) {
        final List<String> preds = r.body().stream().map(this::printPredicate).collect(toList());
        final List<String> expressions = r.expressions().stream().map(this::printExpression).collect(toList());

        String res = String.join(", ", preds);
        if (!expressions.isEmpty()) {
            if (!preds.isEmpty()) {
                res += ", ";
            }
            res += String.join(", ", expressions);
        }

        if (!r.scopes().isEmpty()) {
            res += " trusting ";
            final List<String> scopes = r.scopes().stream().map(this::printScope).collect(toList());
            res += String.join(", ", scopes);
        }
        return res;
    }

    public String printScope(final Scope scope) {
        switch (scope.kind) {
            case Authority:
                return "authority";
            case Previous:
                return "previous";
            case PublicKey:
                Option<PublicKey> pk = this.getPk((int) scope.publicKey);
                if (pk.isDefined()) {
                    return pk.get().toString();
                }
        }
        return "<" + scope.publicKey + "?>";
    }

    public String printSymbol(int i) {
        return getS(i).getOrElse("<" + i + "?>");
    }

    public String printTerm(final Term i) {
        if (i instanceof Term.Variable) {
            return "$" + this.printSymbol((int) ((Term.Variable) i).value());
        } else if (i instanceof Term.Bool) {
            return i.toString();
        } else if (i instanceof Term.Date) {
            return fromEpochIsoDate(((Term.Date) i).value());
        } else if (i instanceof Term.Integer) {
            return "" + ((Term.Integer) i).value();
        } else if (i instanceof Term.Str) {
            return "\"" + this.printSymbol((int) ((Term.Str) i).value()) + "\"";
        } else if (i instanceof Term.Bytes) {
            return "hex:" + Utils.byteArrayToHexString(((Term.Bytes) i).value()).toLowerCase();
        } else if (i instanceof Term.Set) {
            final List<String> values = ((Term.Set) i).value().stream().map(this::printTerm).collect(toList());
            return "[" + String.join(", ", values) + "]";
        } else {
            return "???";
        }
    }

    @SuppressWarnings("unused")
    public String printWorld(final World w) {
        final List<String> facts = w.facts().stream().map(this::printFact).collect(toList());
        final List<String> rules = w.rules().stream().map(this::printRule).collect(toList());

        @SuppressWarnings("StringBufferReplaceableByString")
        StringBuilder b = new StringBuilder();
        b.append("World {\n\tfacts: [\n\t\t");
        b.append(String.join(",\n\t\t", facts));
        b.append("\n\t],\n\trules: [\n\t\t");
        b.append(String.join(",\n\t\t", rules));
        b.append("\n\t]\n}");

        return b.toString();
    }

    public List<PublicKey> publicKeys() {
        return publicKeys;
    }


    @Override
    public int hashCode() {
        int result = dateTimeFormatter.hashCode();
        result = 31 * result + symbols.hashCode();
        result = 31 * result + publicKeys.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SymbolTable that = (SymbolTable) o;

        if (!dateTimeFormatter.equals(that.dateTimeFormatter)) return false;
        if (!symbols.equals(that.symbols)) return false;
        return publicKeys.equals(that.publicKeys);
    }

    @Override
    public String toString() {
        return "SymbolTable{" +
                "symbols=" + symbols +
                ", publicKeys=" + publicKeys +
                '}';
    }
}
