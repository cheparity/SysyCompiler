package middleEnd.visitor.llvm.ir;

public class AllocaInstuction extends Instruction {
    private final IrType allocType;

    AllocaInstuction(IrType allocType) {
        super(OpCode.ALLOCA);
        this.allocType = allocType;
    }

    @Override
    public String toIrCode() {
        return getOpCode().toIrCode() + " " + allocType.toIrCode();
    }
}
