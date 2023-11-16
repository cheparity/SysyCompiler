package middleEnd.visitor.llvm.ir;

/**
 * 形如： @a = dso_local constant i32 5
 * <p></p>
 * <font color='red'>全局定义指令并不作为一个User！这个指令后面肯定是要重构的</font>
 */
public class GlobalDeclInstruction extends Instruction {
    final static String LINKAGE = "dso_local";

    /**
     * 分配指令的值（包含了类型）
     */
    private final Value value;

    /**
     * const or global
     */

    GlobalDeclInstruction(Value value, OpCode code) {
        super();
        //type is useless for store instruction
        super.setOpCode(code);
        this.value = value;
    }

    //@a = dso_local constant i32 5
    //<name> = dso_local <op Code> <type value>
    @Override
    public String toIrCode() {
        return super.getName() + " = " +
                LINKAGE + " " +
                super.getOpCode().toIrCode() +
                " " +
                this.value.toIrCode();
    }
}
