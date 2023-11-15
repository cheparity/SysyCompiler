package middleEnd.visitor.llvm.ir;

public class GlobalDeclInstruction extends Instruction {
    final static String LINKAGE = "dso_local";

    /**
     * const or global
     */

    GlobalDeclInstruction(Value value, OpCode code) {
        super();
        //type is useless for store instruction
        super.setOpCode(code);
        super.addOperand(value);
    }

    //@a = dso_local constant i32 5
    //<name> = dso_local <op Code> <type value>
    @Override
    public String toIrCode() {
        return super.getName() + " = " +
                LINKAGE + " " +
                super.getOpCode().toIrCode() +
                " " +
                super.getOperand(0).toIrCode();
    }
}
