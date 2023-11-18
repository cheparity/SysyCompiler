package middleEnd.llvm.ir;

/**
 * 二元运算指令。[result] = add [ty] [op1], [op2]
 */
public final class BinInstruction extends Instruction {
    private final Variable value1;
    private final Variable value2;
    private final IrType type;
    private final Variable result;
    private final Operator operator;

    BinInstruction(Variable result, Variable value1, Variable value2, Operator operator) {
        this.operator = operator;
        this.result = result;
        this.value1 = value1;
        this.value2 = value2;
        assert value1.getType() == value2.getType();
        this.type = value1.getType();
    }

    @Override
    public String toIrCode() {
        return result + " = " +
                this.operator.toIrCode() +
                " " +
                this.type.toIrCode() +
                " " +
                this.value1.toIrCode() +
                ", " +
                this.value2.toIrCode();
    }

    @Override
    public IrType getType() {
        return type;
    }
}