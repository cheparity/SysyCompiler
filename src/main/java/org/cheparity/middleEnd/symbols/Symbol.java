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
    private int[] dimSize;
    private int dim;
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

    public void setDimSize(int dim, int size) {
        assert dimSize != null && this.dim > 0; //是数组，且大于0维
        dimSize[dim - 1] = size;
    }

    public int getDimSize(int dim) {
        return this.dimSize[dim - 1];
    }

    public int getDim() {
        return dim;
    }

    protected void setDim(int dim) {
        this.dim = dim;
        if (dim == -1) return;
        if (this.dimSize == null) {
            dimSize = new int[dim];
        }
    }

    public Optional<Variable> getIrVariable() {
        return Optional.ofNullable(irVariable);
    }

    /**
     * 如果变量被load出来，可以设置该变量的寄存器但好像还没有写更新寄存器的逻辑，故<font color='red'>先不要用</font>
     *
     * @param irVariable 变量的寄存器
     */
    public void setIrVariable(Variable irVariable) {
        this.irVariable = irVariable;
    }


    /**
     * 如果变量被load出来，可以设置该变量的寄存器，此时变量就有了值。但还没有写更新寄存器的逻辑，故<font color='red'>先不要用</font>
     *
     * @return 变量寄存器中的值
     */
    public Optional<Integer> getNumber() {
        return getNumber(0);
    }

    public Optional<Integer> getNumber(int offset) {
        //数组的情况
        if (getPointer() != null) {
            Integer[] number = getPointer().getNumber();
            if (number == null) return Optional.empty();
            return Optional.of(number[offset]);
        }
        //如果已经被load出来
        if (getIrVariable().isPresent() && getIrVariable().get().readonly) { //修改：增加只读的条件
            return getIrVariable().get().getNumber();
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
