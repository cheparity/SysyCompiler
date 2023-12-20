package middleEnd.llvm.ir;

import middleEnd.os.IrPrintable;
import middleEnd.os.MipsPrintable;

public final class Operator extends User implements MipsPrintable {

    OpCode opCode;

    /**
     * Operator 的 Type 由计算结果决定。比如，两个整数相加，type就是整数。
     *
     * @param type 计算结果的 Type
     */
    private Operator(IrType type) {
        super(type);
    }

    /**
     * 创建一个运算符。应该只会在 {@link BinInstruction} 中调用。
     *
     * @param type   计算结果的 Type
     * @param opCode 操作符
     * @return Operator
     */
    public static Operator create(IrType type, OpCode opCode) {
        Operator operator = new Operator(type);
        operator.opCode = opCode;
        return operator;
    }

    @Override
    public String toIrCode() {
        return this.opCode.toIrCode();
    }

    @Override
    public String toMipsCode() {
        return this.opCode.toMipsCode();
    }

    public enum OpCode implements IrPrintable, MipsPrintable {
        ADD("add", "addu"),       // 加法
        SUB("sub", "subu"),       // 减法
        MUL("mul", "mul"),       // 乘法
        SDIV("sdiv", "div"),     // 有符号整数除法
        //        UDIV("udiv", "div"),     // 无符号整数除法
        SREM("srem", "rem"),     // 有符号整数取模
        //        UREM("urem", "rem"),     // 无符号整数取模
        AND("and", "and"),       // 逻辑与
        OR("or", "or"),         // 逻辑或
        XOR("xor", "not"),       // 逻辑异或
        ICMP("icmp", null),     // 整数比较
        FCMP("fcmp", null),     // 浮点数比较
        LOAD("load", null),     // 加载指令
        STORE("store", null),   // 存储指令
        ALLOCA("alloca", null), // 分配指令
        BR("br", null),         // 分支指令
        SWITCH("switch", null), // 多路分支指令
        RET("ret", null),       // 返回指令
        PHI("phi", null),       // Phi 节点指令
        CONDEF("constant", null), //常量声明指令
        GLODEF("global", null) //全局变量声明指令

        ;
        private final String irValue;
        private final String mipsValue;


        OpCode(String irValue, String mipsValue) {
            this.irValue = irValue;
            this.mipsValue = mipsValue;
        }

        @Override
        public String toIrCode() {
            return irValue;
        }

        @Override
        public String toMipsCode() {
            assert this.mipsValue != null;
            return this.mipsValue;
        }
    }
}
