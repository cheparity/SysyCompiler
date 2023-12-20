package middleEnd.llvm.ir;

/**
 * 二元运算指令。[result] = add [ty] [op1], [op2]
 */
public final class BinInstruction extends Instruction {
    private final Variable value1;
    private final Variable value2;
    private final IrType type;
    private final Variable result;
    private final Operator operator;

    BinInstruction(Variable result, Variable value1, Variable value2, Operator operator) {
        this.operator = operator;
        this.result = result;
        this.value1 = value1;
        this.value2 = value2;
        assert value1.getType().getBasicType() == value2.getType().getBasicType();
        this.type = value1.getType();
    }

    @Override
    public String toIrCode() {
        return result.toIrCode() + " = " +
                this.operator.toIrCode() +
                " " +
                this.type.toIrCode() +
                " " +
                this.value1.toIrCode() +
                ", " +
                this.value2.toIrCode();
    }

    @Override
    public IrType getType() {
        return type;
    }

    @Override
    public String toMipsCode() {
        var sb = new StringBuilder();
        //形如 %3 = add i32 %2, 2
        //就是 load 两个操作数，计算结果，然后 store 回这条指令的位置
        String value1Reg = "$t1", value2Reg = "$t2", resultReg = "$t3";
        if (value1.getNumber().isPresent()) {
            //li $t1, 2
            sb
                    .append(String.format("li\t\t%s, %s", value1Reg, value1.getNumber().get()))
                    .append("\n\t");
        } else {
            //从内存中读取value1Reg
            int offset = getMipsRegisterAllocator().getMemOff(value1.getName());
            sb
                    .append(String.format("lw\t\t%s, %s($fp)", value1Reg, offset))
                    .append("\n\t");
        }
        if (value2.getNumber().isPresent()) {
            //li $t2, 2
            sb
                    .append(String.format("li\t\t%s, %s", value2Reg, value2.getNumber().get()))
                    .append("\n\t");
        } else {
            //从内存中读取value2Reg
            int offset = getMipsRegisterAllocator().getMemOff(value2.getName());
            sb
                    .append(String.format("lw\t\t%s, %s($fp)", value2Reg, offset))
                    .append("\n\t");
        }


        sb
                .append(String.format(String.format("%s\t%s, %s, %s", operator.toMipsCode(), resultReg, value1Reg,
                        value2Reg)))
                .append("\n\t");
        //将resultReg的结果store进去
        int offset = getMipsRegisterAllocator().getMemOff(result.getName());
        sb.append(String.format("sw\t\t%s, %s($fp)", resultReg, offset));
        return sb.toString();
    }
}
