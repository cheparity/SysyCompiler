package middleEnd.symbols;

import frontEnd.lexer.dataStruct.Token;
import middleEnd.llvm.ir.PointerValue;
import middleEnd.llvm.ir.Variable;

import java.util.Optional;

public abstract class Symbol {
    /**
     * The type of this symbol (Var, CONST, FUNC).
     */
    private final SymbolType type;
    /**
     * The symbol table that this symbol belongs to.
     */
    private final SymbolTable symbolTable;
    /**
     * The token that this symbol represents.
     */
    private final Token token;
    /**
     * The register that this symbol is assigned to.
     */
    private Variable irVariable;
    /**
     * 在没有初始化的时候，没有irVariable，只有pointer
     */
    private PointerValue pointer;

    public Symbol(SymbolTable table, SymbolType type, Token token) {
        this.symbolTable = table;
        this.type = type;
        this.token = token;
    }

    public Optional<Variable> getIrVariable() {
        return Optional.ofNullable(irVariable);
    }

    public void setIrVariable(Variable irVariable) {
        this.irVariable = irVariable;
    }

    public Optional<Integer> getNumber() {
        if (irVariable != null) {
            return irVariable.getNumber();
        }
        return Optional.empty();
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


    public PointerValue getPointer() {
        return pointer;
    }

    public void setPointer(PointerValue pointer) {
        this.pointer = pointer;
    }
}
