package middleEnd.visitor.llvm.ir;

public class RetInstruction extends Instruction {
    private final Value retValue;

    RetInstruction(Value result) {
        super(OpCode.RET);
        this.retValue = result;
    }

    @Override
    public String toIrCode() {
        //ret <return_type> <return_value>
        return getOpCode().toIrCode() + retValue.toIrCode();
    }
}
