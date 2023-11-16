package middleEnd.llvm.ir;

/**
 * [resultIdentifier] = alloca [allocType]
 * <p>
 * %1 = alloca [2 x [ 3 x i32]]
 * <p>
 * %6 = alloca i32
 */
public final class AllocaInstruction extends Instruction {
    private final PointerValue operand;

    AllocaInstruction(PointerValue pointerValue) {
        this.operand = pointerValue;
    }

    @Override
    public String toIrCode() {
        return String.format("%s = %s %s", operand.getName(), "alloca", operand.getType().toIrCode());
    }
}
