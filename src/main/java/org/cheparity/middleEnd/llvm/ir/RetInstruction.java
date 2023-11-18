package middleEnd.llvm.ir;

/**
 * ret [type] [value] ,ret void
 * <p>
 * 确实用到了value，后面应该不用重构
 */
public final class RetInstruction extends Instruction {
    private final Variable retValue;
    private final boolean retVoid;

    RetInstruction(Variable result) {
        this.retValue = result;
        retVoid = false;
    }

    RetInstruction() {
        retValue = null;
        retVoid = true;
    }

    @Override
    public String toIrCode() {
        //ret <return_type> <return_value>
        if (retVoid) {
            return "ret void";
        }
        return "ret " + retValue.getType().toIrCode() + " " + retValue.toIrCode();
    }
}
