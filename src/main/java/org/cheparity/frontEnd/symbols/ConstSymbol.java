package frontEnd.symbols;

import frontEnd.lexer.dataStruct.Token;

public final class ConstSymbol extends Symbol {
    private final int dim;
    private int value;

    public ConstSymbol(SymbolTable table, Token token, int dim) {
        super(table, SymbolType.CONST, token);
        this.dim = dim;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getDim() {
        return dim;
    }

}
