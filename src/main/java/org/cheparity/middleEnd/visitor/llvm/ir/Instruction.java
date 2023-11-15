package middleEnd.visitor.llvm.ir;

public abstract class Instruction extends User {
    private OpCode opCode;

    Instruction() {
        super(IrType.VoidTyID);
    }

    Instruction(OpCode opCode) {
        super(IrType.VoidTyID);
        this.opCode = opCode;
    }

    public void addOperand(Value operand) {
        operands.add(operand);
    }

    public Value getOperand(int index) {
        return operands.get(index);
    }

    public OpCode getOpCode() {
        return opCode;
    }

    public void setOpCode(OpCode opCode) {
        this.opCode = opCode;
    }

    @Override
    public String toIrCode() {
        var sb = new StringBuilder();
        sb.append(getName()).append(" = ").append(opCode).append(" ");
        for (int i = 0; i < operands.size(); i++) {
            var operand = operands.get(i);
            sb.append(operand.getType()).append(" ").append(operand.getName());
            if (i != operands.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}
