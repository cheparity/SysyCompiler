package middleEnd.llvm.ir;

/**
 * [result] = load [(optional)volatile] [ty], ptr [pointer]
 */
public final class LoadInstruction extends Instruction {
    private final Variable result;
    private final IrType type;
    private final PointerValue pointerValue;


    public LoadInstruction(Variable result, PointerValue pointerValue) {
        this.result = result;
        this.type = pointerValue.getType();
        this.pointerValue = pointerValue;
    }

    @Override
    public String toIrCode() {
        return result.toIrCode() + " = load " + type.toIrCode() + ", " + pointerValue.getType().toIrCode() + "* " + pointerValue.getName();
    }

    @Override
    public String toMipsCode() {
        var sb = new StringBuilder();
        String t1 = "$t1";
        if (pointerValue.getName().startsWith("@")) { //全局变量
            String pointerName = pointerValue.getName().substring(1);
            sb.append(String.format("lw\t\t%s, %s", t1, pointerName)).append("\n\t");
            if (getMipsRegisterAllocator().getFpMemOff(result.getName()) != null) {
                sb.append("sw\t\t$t1, ").append(getMipsRegisterAllocator().getFpMemOff(result.getName())).append("($fp)"); //有的话，存进地址
            } else {
                sb
                        .append("addiu\t$sp, $sp, -4") //分配新变量的空间
                        .append("\n\t")
                        .append("sw\t\t$t1, ($sp)"); //把值（注意是值！！）存进新变量
                getMipsRegisterAllocator().addFpOffset(result.getName());
            }
        }
        Integer fpMemOff = getMipsRegisterAllocator().getFpMemOff(pointerValue.getName());
        sb
                .append("lw\t\t$t0, ").append(fpMemOff).append("($fp)")
                .append("\n\t")
                .append("lw\t\t$t1, ($t0)")
                .append("\n\t");
        if (getMipsRegisterAllocator().getFpMemOff(result.getName()) != null) {
            sb.append("sw\t\t$t1, ").append(getMipsRegisterAllocator().getFpMemOff(result.getName())).append("($fp)"); //有的话，存进地址
        } else {
            sb
                    .append("addiu\t$sp, $sp, -4") //分配新变量的空间
                    .append("\n\t")
                    .append("sw\t\t$t1, ($sp)"); //把值（注意是值！！）存进新变量
            getMipsRegisterAllocator().addFpOffset(result.getName());
        }


        return sb.toString();
    }
}
