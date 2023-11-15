package frontEnd.symbols;

import frontEnd.lexer.dataStruct.Token;

import java.util.List;

public final class FuncSymbol extends Symbol {
    private final FuncType funcType; //void int
    private List<VarSymbol> params;
    private int dim;

    public FuncSymbol(SymbolTable table, Token token, FuncType funcType) {
        super(table, SymbolType.FUNC, token);
        this.funcType = funcType;
    }


    public FuncType getFuncType() {
        return funcType;
    }


    public int getDim() {
        return dim;
    }

    public void setDim(FuncType type) {
        if (type == FuncType.VOID) {
            this.dim = -1;
        } else if (type == FuncType.INT) {
            this.dim = 0;
        }
    }

    public int getParamCount() {
        return params.size();
    }

    public List<VarSymbol> getParams() {
        return this.params;
    }

    public void setFParams(List<VarSymbol> fparams) {
        this.params = fparams;
    }
}
