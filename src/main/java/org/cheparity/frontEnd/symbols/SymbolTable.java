package frontEnd.symbols;

import exception.DupIdentError;
import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.ErrorHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class SymbolTable {
    private static SymbolTable global;
    /**
     * The outer symbol table of this symbol table. To search outer symbols.
     */
    private final SymbolTable outer;
    /**
     * The inner symbol tables of this symbol table. To store inner symbols.
     */
    private final List<SymbolTable> inners = new ArrayList<>();
    /**
     * The level of this symbol table.
     */
    private final int level;
    /**
     * The directory of this symbol table. To store symbols.
     */
    private final HashMap<String, Symbol> directory = new HashMap<>();

    public SymbolTable(SymbolTable outer, ASTNode belongTo) {
        this.outer = outer;
        if (outer == null) {
            this.level = 0; //global
        } else {
            this.level = outer.level + 1;
            outer.addInnerTable(this);
        }
        belongTo.setSymbolTable(this);
    }

    public static SymbolTable getGlobal() {
        return global;
    }

    public static void setGlobal(SymbolTable global) {
        SymbolTable.global = global;
    }

    private void addInnerTable(SymbolTable inner) {
        inners.add(inner);
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

    /**
     * Lookup a symbol in this symbol table and its outer symbol tables.
     * <p>
     * Both for variable and function.
     *
     * @param name The name of the symbol.(key)
     * @return The symbol if found, otherwise null.(value)
     */
    public Optional<Symbol> lookup(String name) {
        if (directory.containsKey(name)) {
            return Optional.of(directory.get(name));
        }
        if (outer != null) {
            return outer.lookup(name);
        }
        return Optional.empty();
    }

    /**
     * Lookup a function variable in this symbol table and its outer symbol tables.
     *
     * @param name The name of the function.(key)
     * @return The function symbol if found, otherwise null.(value)
     */
    public Optional<FuncSymbol> lookup4func(String name) {
        Optional<Symbol> res = lookup(name);
        if (res.isPresent() && res.get().getType() == SymbolType.FUNC) return Optional.of((FuncSymbol) res.get());
        return Optional.empty();
    }

}
