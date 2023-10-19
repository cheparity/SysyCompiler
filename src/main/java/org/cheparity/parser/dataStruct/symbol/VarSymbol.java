package parser.dataStruct.symbol;

import lexer.dataStruct.Token;

public final class VarSymbol extends Symbol {
    private final int dim;

    public VarSymbol(SymbolTable table, Token token, int dim) {
        super(table, SymbolType.VAR, token);
        this.dim = dim;
    }

    public int getDim() {
        return dim;
    }
}
