package org.biscuitsec.biscuit.datalog;

import io.vavr.control.Option;

import java.util.ArrayList;
import java.util.List;

import static org.biscuitsec.biscuit.datalog.SymbolTable.DEFAULT_SYMBOLS_OFFSET;

public class TemporarySymbolTable {
    final SymbolTable base;
    final int offset;
    final List<String> symbols;

    public TemporarySymbolTable(SymbolTable base) {
        this.offset = DEFAULT_SYMBOLS_OFFSET + base.currentOffset();
        this.base = base;
        this.symbols = new ArrayList<>();
    }

    public Option<String> getS(int i) {
        if (i >= this.offset) {
            if (i - this.offset < this.symbols.size()) {
                return Option.some(this.symbols.get(i - this.offset));
            } else {
                return Option.none();
            }
        } else {
            return this.base.getS(i);
        }
    }

    public long insert(final String symbol) {
        Option<Long> opt = this.base.get(symbol);
        if (opt.isDefined()) {
            return opt.get();
        }

        int index = this.symbols.indexOf(symbol);
        if (index != -1) {
            return this.offset + index;
        }
        this.symbols.add(symbol);
        return this.symbols.size() - 1 + this.offset;
    }
}
