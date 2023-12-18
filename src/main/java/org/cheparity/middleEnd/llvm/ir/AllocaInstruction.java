package middleEnd.llvm.ir;

import middleEnd.llvm.MipsRegisterAllocator;

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

    @Override
    public String toMipsCode() {
        var sb = new StringBuilder();
        int offset = MipsRegisterAllocator.allocaMem(operand.getName());
        sb.append("addiu\t$sp, $sp, ").append(offset);
        return sb.toString();
    }
}
