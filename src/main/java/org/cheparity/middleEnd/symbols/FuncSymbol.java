package middleEnd.symbols;

import frontEnd.lexer.dataStruct.Token;
import middleEnd.llvm.ir.IrFunction;

import java.util.List;

public final class FuncSymbol extends Symbol {
    private final FuncType funcType; //void int
    private List<VarSymbol> params;
    private IrFunction irFunction;

    public FuncSymbol(SymbolTable table, Token token, FuncType funcType) {
        super(table, SymbolType.FUNC, token);
        this.funcType = funcType;
    }

    public FuncType getFuncType() {
        return funcType;
    }

    public void setDim(FuncType type) {
        if (type == FuncType.VOID) {
            super.setDim(-1);
        } else if (type == FuncType.INT) {
            super.setDim(0);
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

    public IrFunction getFunction() {
        return irFunction;
    }

    public void setFunction(IrFunction irFunction) {
        this.irFunction = irFunction;
    }
}
