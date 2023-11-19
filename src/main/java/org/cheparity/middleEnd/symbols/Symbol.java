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
     * 局部变量<font color='red'>load进寄存器</font>时，或者<font color='red'>全局变量初始化</font>时，初始化irVariable。
     */
    private Variable irVariable;
    /**
     * 符号的地址。<font color='red'>当初始化符号时，初始化地址；当给符号赋值时调用store指令，也是给地址赋值。</font>
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
        return this.pointer;
    }

    public void setPointer(PointerValue pointer) {
        this.pointer = pointer;
    }
}
