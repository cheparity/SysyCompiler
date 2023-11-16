package middleEnd.llvm.ir;

/**
 * ret [type] [value] ,ret void
 * <p>
 * 确实用到了value，后面应该不用重构
 */
public final class RetInstruction extends Instruction {
    private final Variable retValue;

    RetInstruction(Variable result) {
        this.retValue = result;
    }

    @Override
    public String toIrCode() {
        //ret <return_type> <return_value>
        return "ret " + retValue.toIrCode();
    }
}
