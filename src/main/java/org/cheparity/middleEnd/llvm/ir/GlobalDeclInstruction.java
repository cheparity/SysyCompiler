package middleEnd.llvm.ir;

import utils.LoggerUtil;

import java.util.logging.Logger;

/**
 * 形如： @a = dso_local constant i32 5
 * <p></p>
 * <font color='red'>全局定义指令并不作为一个User！这个指令后面肯定是要重构的</font>
 */
public final class GlobalDeclInstruction extends Instruction {
    private final static Logger LOGGER = LoggerUtil.getLogger();

    /**
     * 分配指令的值（包含了类型）
     */
    private final GlobalValue variable;

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
    GlobalDeclInstruction(GlobalValue variable, Boolean constant) {
        super();
        //type is useless for store instruction
        this.variable = variable;
        modifier = constant ? "constant" : "global";
    }

    //@a = dso_local constant i32 5
    //<name> = dso_local <op Code> <type value>
    @Override
    public String toIrCode() {
        if (!variable.getType().isArray()) {
            if (variable.getNumber() == null) {
                //就没有用过
                LOGGER.warning("Undeclared variable.");
                return String.format("%s = dso_local %s %s %s",
                        variable.getName(),
                        modifier,
                        variable.getType().toIrCode(),
                        "zeroinitializer"
                );
            }
            return String.format("%s = dso_local %s %s %s", variable.getName(), modifier, variable.getType().toIrCode(),
                    variable.getNumber()[0]);
        }
        //@b = dso_local global [100 x i32]] zeroinitializer
        //@a = dso_local global [10 x i32] [i32 1, i32 2, i32 3, i32 0, i32 0, i32 0, i32 0, i32 0, i32 0, i32 0]
        StringBuilder sb = new StringBuilder();
        var type = variable.getType();
        var basicType = type.getBasicType();
        sb.append(variable.getName()).append(" = dso_local ").append(modifier).append(" ").append(type.toIrCode()).append(" ");
        boolean isAllZero = true;
        for (Integer num : variable.getNumber()) {
            if (!num.equals(0)) {
                isAllZero = false;
                break;
            }
        }
        if (isAllZero) {
            //如果初值全是0，那么就用zeroinitializer
            sb.append("zeroinitializer");
        } else {
            sb.append("[");
            for (int i = 0; i < variable.getNumber().length; i++) {
                if (i != 0) {
                    sb.append(", ");
                }
                sb.append(basicType.toIrCode()).append(" ").append(variable.getNumber()[i]);
            }
            sb.append("]");
        }
        return sb.toString();
    }
}
