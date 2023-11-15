package middleEnd.visitor.llvm.ir;

import middleEnd.visitor.os.IrPrintable;

public enum OpCode implements IrPrintable {
    ADD("add"),       // 加法
    SUB("sub"),       // 减法
    MUL("mul"),       // 乘法
    SDIV("sdiv"),     // 有符号整数除法
    UDIV("udiv"),     // 无符号整数除法
    SREM("srem"),     // 有符号整数取模
    UREM("urem"),     // 无符号整数取模
    AND("and"),       // 逻辑与
    OR("or"),         // 逻辑或
    XOR("xor"),       // 逻辑异或
    ICMP("icmp"),     // 整数比较
    FCMP("fcmp"),     // 浮点数比较
    LOAD("load"),     // 加载指令
    STORE("store"),   // 存储指令
    ALLOCA("alloca"), // 分配指令
    BR("br"),         // 分支指令
    SWITCH("switch"), // 多路分支指令
    RET("ret"),       // 返回指令
    PHI("phi"),       // Phi 节点指令
    CONDEF("constant"), //常量声明指令
    GLODEF("global") //全局变量声明指令

    ;
    private final String stringValue;


    OpCode(String stringValue) {
        this.stringValue = stringValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    @Override
    public String toIrCode() {
        return stringValue;
    }
}