package parser.dataStruct.symbol;

import lexer.dataStruct.Token;

public final class VarSymbol extends Symbol {
    private int dim;

    public VarSymbol(SymbolTable table, Token token) {
        super(table, SymbolType.VAR, token);
    }

    public int getDim() {
        return dim;
    }

    public void setDim(int dim) {
        this.dim = dim;
    }
}
