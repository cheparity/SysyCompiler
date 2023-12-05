package middleEnd.symbols;

import frontEnd.lexer.dataStruct.Token;

public final class VarSymbol extends Symbol {
    public VarSymbol(SymbolTable table, Token token, int dim) {
        super(table, SymbolType.VAR, token);
        super.setDim(dim);
    }
}
