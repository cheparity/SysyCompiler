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
    /**
     * The register that this symbol is assigned to.
     */
    private String register;

    public Symbol(SymbolTable table, SymbolType type, Token token) {
        this.symbolTable = table;
        this.type = type;
        this.token = token;
    }

    public String getRegister() {
        return this.register;
    }

    public void setRegister(String register) {
        this.register = register;
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
