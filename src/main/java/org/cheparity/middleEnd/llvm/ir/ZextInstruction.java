package middleEnd.llvm.ir;

//%6 = zext i1 %5 to i32
public final class ZextInstruction extends Instruction {
    Variable rawVariable;
    Variable result;

    ZextInstruction(Variable rawVariable, Variable result) {
        this.rawVariable = rawVariable;
        this.result = result;
    }

    @Override
    public String toIrCode() {
        return String.format("%s = zext %s %s to %s",
                result.getName(),
                rawVariable.getType().getBasicType().toIrCode(),
                rawVariable.getName(),
                result.getType().getBasicType().toIrCode()
        );
    }

    @Override
    public String toMipsCode() {
        //直接load出来然后store即可
        var sb = new StringBuilder();
        Integer varOff = getMipsRegisterAllocator().getFpMemOff(rawVariable.getName());
        Integer resOff = getMipsRegisterAllocator().getFpMemOff(result.getName());
        sb
                .append("lw\t\t$t0, ").append(varOff).append("($fp)")
                .append("\n\t")
                .append("sw\t\t$t0, ").append(resOff).append("($fp)");
        return sb.toString();

    }
}
