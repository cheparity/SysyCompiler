package middleEnd.visitor.llvm.ir;

/**
 * [resultIdentifier] = alloca [allocType]
 * <p>
 * %1 = alloca [2 x [ 3 x i32]]
 * <p>
 * %6 = alloca i32
 */
public class AllocaInstruction extends Instruction {
    private final PointerValue operand;

    AllocaInstruction(PointerValue pointerValue) {
        super(OpCode.ALLOCA); // opcode为alloca
        this.operand = pointerValue;
    }

    @Override
    public String toIrCode() {
        return String.format("%s = %s %s", operand.getName(), getOpCode(), operand.getType().toIrCode());
    }
}
