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

    @Override
    public String toMipsCode() {
        var offset = operand.getType().getMemByteSize();
        String sb = "addiu\t$sp, $sp, " + -offset + //alloca了一段空间。t0存放空间的起始地址
                "\n\t" +
                "la\t\t$t0, " + "($sp)" +
                "\n\t" +
                "addiu\t$sp, $sp, -4" +
                "\n\t" +
                "sw\t\t$t0, ($sp)";
        getMipsRegisterAllocator().addFpOffset(operand.getName(), offset);
        return sb;
    }
}
