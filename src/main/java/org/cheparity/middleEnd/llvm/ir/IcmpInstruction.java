package middleEnd.llvm.ir;

import middleEnd.os.IrPrintable;

/**
 * [result] = icmp [cond] [ty] [op1], [op2]   ; yields i1 or [N x i1]:result
 */
public class IcmpInstruction extends Instruction {
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
        return "icmp " + cond.toIrCode() + " " + op1.getType().toIrCode() + " " + op1.toIrCode() + ", " + op2.toIrCode();
    }

    public enum Cond implements IrPrintable {
        /**
         * equal
         */
        EQ("eq"),
        /**
         * not equal
         */
        NE("ne"),
        /**
         * unsigned greater than
         */
        UGT("ugt"),
        /**
         * unsigned greater or equal
         */
        UGE("uge"),
        /**
         * unsigned less than
         */
        ULT("ult"),
        /**
         * unsigned less or equal
         */
        ULE("ule"),
        /**
         * signed greater than
         */
        SGT("sgt"),
        /**
         * signed greater or equal
         */
        SGE("sge"),
        /**
         * signed less than
         */
        SLT("slt"),
        /**
         * signed less than or equal
         */
        SLE("sle");
        final String value;

        Cond(String value) {
            this.value = value;
        }

        @Override
        public String toIrCode() {
            return this.value;
        }
    }
}
