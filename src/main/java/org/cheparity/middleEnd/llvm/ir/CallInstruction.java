package middleEnd.llvm.ir;

/**
 * call void @putch(i32 104)
 */
public final class CallInstruction extends Instruction {
    private final Function function;
    private final Variable[] args;
    private Variable receiver;

    /**
     * 有接受者的函数调用
     *
     * @param function 函数
     * @param receiver 接受者
     * @param args     参数
     */
    public CallInstruction(Function function, Variable receiver, Variable... args) {
        this.function = function;
        this.args = args;
        this.receiver = receiver;
    }

    /**
     * 无接受者的函数调用
     *
     * @param function 函数
     * @param args     参数
     */
    public CallInstruction(Function function, Variable... args) {
        this.function = function;
        this.args = args;
    }


    @Override
    public String toIrCode() {
        var sb = new StringBuilder();
        if (receiver != null) sb.append(receiver.getName()).append(" = ");
        sb.append("call ").append(function.getType().toIrCode()).append(" ").append(function.getName()).append(
                "(");
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i].getType().toIrCode()).append(" ").append(args[i].getName());
            if (i != args.length - 1) sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }

    Variable getReceiver() {
        return receiver;
    }
}
