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
        var sb = new StringBuilder();
//        getMipsRegisterAllocator().allocaMem(operand.getName(), offset);
        Integer memOff = getMipsRegisterAllocator().getMemOff(operand.getName());
//        sb.append("addiu\t$sp, $sp, ").append(-offset);
        sb
                .append("la\t\t$t0, ").append(memOff).append("($fp)")
                .append("\n\t")
                .append("sw\t\t$t0, ").append(memOff).append("($fp)"); //确保alloca指令存的都是地址

//        return sb.toString();
        return sb.toString();
    }
}
