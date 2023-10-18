package parser.dataStruct.symbol;

import lexer.dataStruct.Token;

public final class ConstSymbol extends Symbol {
    public ConstSymbol(SymbolTable table, Token token) {
        super(table, SymbolType.CONST, token);
    }
}
