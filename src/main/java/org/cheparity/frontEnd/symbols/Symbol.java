package frontEnd.symbols;

import frontEnd.lexer.dataStruct.Token;

public abstract class Symbol {
    /**
     * The type of this symbol (Var, CONST, FUNC).
     */
    private final SymbolType type;
    /**
     * The symbol table that this symbol belongs to.
     */
    private final SymbolTable symbolTable;
    /**
     * The token that this symbol represents.
     */
    private final Token token;

    public Symbol(SymbolTable table, SymbolType type, Token token) {
        this.symbolTable = table;
        this.type = type;
        this.token = token;
    }

    public Token getToken() {
        return this.token;
    }

    public SymbolTable getSymbolTable() {
        return this.symbolTable;
    }

    public SymbolType getType() {
        return this.type;
    }


}
