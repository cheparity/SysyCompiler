package middleEnd.visitor.llvm.ir;

/**
 * ret [type] [value] ,ret void
 * <p>
 * 确实用到了value，后面应该不用重构
 */
public final class RetInstruction extends Instruction {
    private final DataValue retValue;

    RetInstruction(DataValue result) {
        super(OpCode.RET);
        this.retValue = result;
    }

    @Override
    public String toIrCode() {
        //ret <return_type> <return_value>
        return getOpCode().toIrCode() + retValue.toIrCode();
    }
}
