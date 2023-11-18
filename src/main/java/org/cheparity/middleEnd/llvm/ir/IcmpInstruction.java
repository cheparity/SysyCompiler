package middleEnd.llvm.ir;

import middleEnd.os.IrPrintable;

/**
 * [result] = icmp [cond] [ty] [op1], [op2]   ; yields i1 or [N x i1]:result
 */
public class IcmpInstruction extends Instruction {
    private final Variable result;
    private final Variable op1;
    private final Variable op2;
    private final Condition cond;

    public IcmpInstruction(Variable result, Variable op1, Variable op2, Condition condition) {
        this.result = result;
        this.op1 = op1;
        this.op2 = op2;
        this.cond = condition;
    }

    @Override
    public String toIrCode() {
        return "icmp " + cond.toIrCode() + " " + op1.getType().toIrCode() + " " + op1.toIrCode() + ", " + op2.toIrCode();
    }

    static class Condition implements IrPrintable {
        private final Cond cond;

        Condition(Cond cond) {
            this.cond = cond;
        }

        @Override
        public String toIrCode() {
            return this.cond.value;
        }

        enum Cond {
            EQ("eq"),
            NE("ne"),
            UGT("ugt"),
            UGE("uge"),
            ULT("ult"),
            ULE("ule"),
            SGT("sgt"),
            SGE("sge"),
            SLT("slt"),
            SLE("sle");
            final String value;

            Cond(String value) {
                this.value = value;
            }

        }
    }
}
