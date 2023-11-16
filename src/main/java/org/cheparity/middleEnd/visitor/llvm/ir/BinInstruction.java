package middleEnd.visitor.llvm.ir;

/**
 * 二元运算指令。[result] = add [ty] [op1], [op2]
 */
public class BinInstruction extends Instruction {
    private final Value value1;
    private final Value value2;
    private final IrType type;
    private final String result;

    BinInstruction(String result, Value value1, Value value2, OpCode opCode) {
        super(opCode);
        this.result = result;
        this.value1 = value1;
        this.value2 = value2;
        assert value1.getType() == value2.getType();
        this.type = value1.getType();
    }

    @Override
    public String toIrCode() {
        return result + " = " +
                super.getOpCode().toIrCode() +
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
