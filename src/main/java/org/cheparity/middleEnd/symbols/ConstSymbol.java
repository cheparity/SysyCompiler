package middleEnd.symbols;

import frontEnd.lexer.dataStruct.Token;

public final class ConstSymbol extends Symbol {
    private int value;

    public ConstSymbol(SymbolTable table, Token token, int dim) {
        super(table, SymbolType.CONST, token);
        super.setDim(dim);
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

}
