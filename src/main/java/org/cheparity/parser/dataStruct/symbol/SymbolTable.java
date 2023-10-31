package parser.dataStruct.symbol;

import exception.DupIdentError;
import parser.dataStruct.ErrorHandler;

import java.util.HashMap;
import java.util.Optional;

public class SymbolTable {
    private final SymbolTable outer;
    private final int level;
    private final HashMap<String, Symbol> directory = new HashMap<>();

    public SymbolTable(SymbolTable outer) {
        this.outer = outer;
        if (outer == null) {
            this.level = 0; //global
        } else {
            this.level = outer.level + 1;
        }
    }

    public SymbolTable getOuter() {
        return outer;
    }

    public void addSymbol(Symbol symbol, ErrorHandler errorHandler) {
        String name = symbol.getToken().getRawValue();
        if (!directory.containsKey(name)) {
            this.directory.put(name, symbol);
            return;
        }
        var token = symbol.getToken();
        var preToken = directory.get(name).getToken();
        errorHandler.addError(new DupIdentError(token, preToken));
    }

    public Optional<Symbol> lookup(String name) {
        if (directory.containsKey(name)) {
            return Optional.of(directory.get(name));
        }
        if (outer != null) {
            return outer.lookup(name);
        }
        return Optional.empty();
    }

    public Optional<FuncSymbol> lookup4func(String name) {
        Optional<Symbol> res = lookup(name);
        if (res.isPresent() && res.get().getType() == SymbolType.FUNC) return Optional.of((FuncSymbol) res.get());
        return Optional.empty();
    }
}
