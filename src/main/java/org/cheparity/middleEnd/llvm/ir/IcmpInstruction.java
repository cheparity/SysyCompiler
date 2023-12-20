package middleEnd.llvm.ir;

import middleEnd.os.IrPrintable;
import middleEnd.os.MipsPrintable;

/**
 * [result] = icmp [cond] [ty] [op1], [op2]   ; yields i1 or [N x i1]:result
 */
public final class IcmpInstruction extends Instruction {
    final Variable result;
    final Variable op1;
    final Variable op2;
    final Cond cond;

    public IcmpInstruction(Variable result, Variable op1, Variable op2, Cond condition) {
        this.result = result;
        this.op1 = op1;
        this.op2 = op2;
        this.cond = condition;
    }

    @Override
    public String toIrCode() {
        return String.format("%s = icmp %s %s %s, %s", result.toIrCode(), cond.toIrCode(), op1.getType().toIrCode(), op1.toIrCode(), op2.toIrCode());
    }

    @Override
    public String toMipsCode() {
        var sb = new StringBuilder();
        //形如 %3 = add i32 %2, 2
        //就是 load 两个操作数，计算结果，然后 store 回这条指令的位置
        String op1Reg = "$t1", op2Reg = "$t2", resultReg = "$t3";
        if (op1.getNumber().isPresent()) {
            //li $t1, 2
            sb
                    .append(String.format("li\t\t%s, %s", op1Reg, op1.getNumber().get()))
                    .append("\n\t");
        } else {
            //从内存中读取 op1Reg
            int offset = getMipsRegisterAllocator().getMemOff(op1.getName());
            sb
                    .append(String.format("lw\t\t%s, %s($fp)", op1Reg, offset))
                    .append("\n\t");
        }
        if (op2.getNumber().isPresent()) {
            //li $t2, 2
            sb
                    .append(String.format("li\t\t%s, %s", op2Reg, op2.getNumber().get()))
                    .append("\n\t");
        } else {
            //从内存中读取 op2Reg
            int offset = getMipsRegisterAllocator().getMemOff(op2.getName());
            sb
                    .append(String.format("lw\t\t%s, %s($fp)", op2Reg, offset))
                    .append("\n\t");
        }

        sb
                .append(String.format(String.format("%s\t\t%s, %s, %s", cond.toMipsCode(), resultReg, op1Reg,
                        op2Reg)))
                .append("\n\t");
        //将resultReg的结果store进去
        int offset = getMipsRegisterAllocator().getMemOff(result.getName());
        sb.append(String.format("sw\t\t%s, %s($fp)", resultReg, offset));
        return sb.toString();
    }

    public enum Cond implements IrPrintable, MipsPrintable {
        /**
         * equal
         */
        EQ("eq", "seq"),
        /**
         * not equal
         */
        NE("ne", "sne"),
        /**
         * unsigned greater than
         */
        UGT("ugt", "sgtu"),
        /**
         * unsigned greater or equal
         */
        UGE("uge", "sgeu"),
        /**
         * unsigned less than
         */
        ULT("ult", "sltu"),
        /**
         * unsigned less or equal
         */
        ULE("ule", "sleu"),
        /**
         * signed greater than
         */
        SGT("sgt", "sgt"),
        /**
         * signed greater or equal
         */
        SGE("sge", "sge"),
        /**
         * signed less than
         */
        SLT("slt", "slt"),
        /**
         * signed less than or equal
         */
        SLE("sle", "sle");
        final String value;
        final String mipsValue;

        Cond(String irValue, String mipsValue) {
            this.value = irValue;
            this.mipsValue = mipsValue;
        }

        @Override
        public String toIrCode() {
            return this.value;
        }


        @Override
        public String toMipsCode() {
            return this.mipsValue;
        }
    }
}
