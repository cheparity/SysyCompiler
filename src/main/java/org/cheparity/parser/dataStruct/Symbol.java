package parser.dataStruct;

import lexer.dataStruct.Token;

public class Symbol {
    private final SymbolType type;
    private final SymbolTable symbolTable;
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
