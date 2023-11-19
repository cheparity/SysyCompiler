package middleEnd.llvm.ir;

/**
 * [result] = load [(optional)volatile] [ty], ptr [pointer]
 */
public final class LoadInstruction extends Instruction {
    private Variable result;
    private IrType type;
    private PointerValue pointerValue;


    public LoadInstruction(Variable result, PointerValue pointerValue) {
        this.result = result;
        this.type = pointerValue.getType();
        this.pointerValue = pointerValue;
    }

    @Override
    public String toIrCode() {
        return result.toIrCode() + " = load " + type.toIrCode() + ", " + pointerValue.getType().toIrCode() + "* " + pointerValue.getName();
    }
}
