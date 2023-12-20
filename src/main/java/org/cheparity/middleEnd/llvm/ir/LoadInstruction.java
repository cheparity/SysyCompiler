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
        if (pointerValue.getName().startsWith("@")) {
            String pointerName = pointerValue.getName().substring(1);
            sb.append(String.format("lw\t\t%s, %s", t1, pointerName));
            //放到内存里，先lw，再sw，这种废物的做法只有我能写出来
            int offset = getMipsRegisterAllocator().getMemOff(result.getName());
            sb
                    .append("\n\t")
                    .append(String.format("sw\t\t%s, %s($fp)", t1, offset));
            return sb.toString();
        }
        Integer offset = getMipsRegisterAllocator().getMemOff(pointerValue.getName());
        sb
                .append(String.format("lw\t\t%s, %s($fp)", t1, offset))
                .append("\n\t")
                .append(String.format("sw\t\t%s, %s($fp)", t1, getMipsRegisterAllocator().getMemOff(result.getName())));


        return sb.toString();
    }
}
