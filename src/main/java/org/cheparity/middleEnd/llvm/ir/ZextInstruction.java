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
}
