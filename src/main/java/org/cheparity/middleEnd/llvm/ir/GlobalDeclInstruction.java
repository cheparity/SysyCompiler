package middleEnd.llvm.ir;

/**
 * 形如： @a = dso_local constant i32 5
 * <p></p>
 * <font color='red'>全局定义指令并不作为一个User！这个指令后面肯定是要重构的</font>
 */
public final class GlobalDeclInstruction extends Instruction {

    /**
     * 分配指令的值（包含了类型）
     */
    private final GlobalVariable variable;

    /**
     * const or global
     */
    private final String modifier;

    /**
     * 全局常量（变量）声明语句
     *
     * @param variable 要声明的变量。一般，在全局声明的变量以原名命名
     * @param constant bool，是否是全局常量（决定了dec语句的modifier是constant/global）
     */
    GlobalDeclInstruction(GlobalVariable variable, Boolean constant) {
        super();
        //type is useless for store instruction
        this.variable = variable;
        modifier = constant ? "constant" : "global";
    }

    //@a = dso_local constant i32 5
    //<name> = dso_local <op Code> <type value>
    @Override
    public String toIrCode() {
        return String.format("%s = dso_local %s %s", variable.getName(), modifier, variable.toIrCode());
    }
}
