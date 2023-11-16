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

    public OpCode getOpCode() {
        return opCode;
    }

    public void setOpCode(OpCode opCode) {
        this.opCode = opCode;
    }

}
