package parser.dataStruct.symbol;

import lexer.dataStruct.Token;

public final class ConstSymbol extends Symbol {
    private final int dim;

    public ConstSymbol(SymbolTable table, Token token, int dim) {
        super(table, SymbolType.CONST, token);
        this.dim = dim;
    }

    public int getDim() {
        return dim;
    }

}
