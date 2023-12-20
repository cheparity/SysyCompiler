package middleEnd.llvm.ir;

/**
 * call void @putch(i32 104)
 */
public final class CallInstruction extends Instruction {
    private final IrFunction irFunction;
    private final Variable[] args;
    private Variable receiver;

    /**
     * 有接受者的函数调用
     *
     * @param irFunction 函数
     * @param receiver   接受者
     * @param args       参数
     */
    public CallInstruction(IrFunction irFunction, Variable receiver, Variable... args) {
        this.irFunction = irFunction;
        this.args = args;
        this.receiver = receiver;
    }

    /**
     * 无接受者的函数调用
     *
     * @param irFunction 函数
     * @param args       参数
     */
    public CallInstruction(IrFunction irFunction, Variable... args) {
        this.irFunction = irFunction;
        this.args = args;
    }


    @Override
    public String toIrCode() {
        var sb = new StringBuilder();
        if (receiver != null) sb.append(receiver.getName()).append(" = ");
        sb.append("call ").append(irFunction.getType().toIrCode()).append(" ").append(irFunction.getName()).append(
                "(");
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i].getType().toIrCode()).append(" ").append(args[i].getName());
            if (i != args.length - 1) sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }

    /*
     *
     * -------------------
    |                   |
    |                   |
    |       Stack       |
    |                   |
    |                   |
     -------------------         <----fp
    |       ...         |
    |  Caller's Stack   |
    |       ...         |
     -------------------
    |    Return Addr    |
     -------------------
    |    Caller's FP     |
     -------------------
    |                   |
    |     Arguments     |
    |                   |
     -------------------
    |                   |
    |  Caller's Local   |
    |       Vars        |
    |                   |
     -------------------
     *
     * */
    @Override
    public String toMipsCode() {
        //记住：call的时候保存现场，return的时候恢复现场
        String funcName = irFunction.getName().substring(1);
        var sb = new StringBuilder();

        //判断是否是putch或getint
        switch (funcName) {
            case "putch" -> {
                assert args[0].getNumber().isPresent();
                sb
                        .append("li\t\t$a0, ").append(args[0].getNumber().get())
                        .append("\n\t")
                        .append("li\t\t$v0, 11")
                        .append("\n\t")
                        .append("syscall");
                return sb.toString();
            }
            case "putint" -> {
                if (args[0].getNumber().isPresent()) {
                    sb
                            .append(String.format("li\t\t$a0, %s", args[0].getNumber().get()))
                            .append("\n\t")
                            .append("li\t\t$v0, 1")
                            .append("\n\t")
                            .append("syscall");
                } else {
                    String name = args[0].getName();
                    Integer memOff = getMipsRegisterAllocator().getFpMemOff(name);
                    sb
                            .append("\n\t")
                            .append("lw\t\t$a0, ").append(memOff).append("($fp)")
                            .append("\n\t")
                            .append("li\t\t$v0, 1")
                            .append("\n\t")
                            .append("syscall");
                }
                return sb.toString();
            }
            case "getint" -> {
                sb
                        .append("li\t\t$v0, 5")
                        .append("\n\t")
                        .append("syscall");
                //完了你还要存起来
                String receiverName = receiver.getName();
                if (getMipsRegisterAllocator().getFpMemOff(receiverName) != null) {
                    sb
                            .append("\n\t")
                            .append("sw\t\t$v0, ").append(getMipsRegisterAllocator().getFpMemOff(receiverName)).append("($fp)");
                    return sb.toString();
                }

                getMipsRegisterAllocator().addFpOffset(receiverName);
                sb
                        .append("\n\t")
                        .append("addiu\t$sp, $sp, -4")
                        .append("\n\t")
                        .append("sw\t\t$v0, ($sp)");
                return sb.toString();
            }
        }

        //首先要把return addr 存起来
        sb
                .append("addi\t$sp, $sp, -4") //存return add
                .append("\n\t")
                .append("sw\t\t$ra, 0($sp)");

        //存当前的fp
        sb
                .append("\n\t")
                .append("addi\t$sp, $sp, -4")
                .append("\n\t")
                .append("sw\t\t$fp, 0($sp)");


        //保存参数，不要移动fp指针了
        int off = 0;
        for (var arg : args) {
            off -= 4;
            //如果是地址，得用la指令
            if (arg.getType().isPointer()) {
                Integer memOff = getMipsRegisterAllocator().getFpMemOff(arg.getName());
                sb
                        .append("\n\t")
                        .append(String.format("lw\t\t$t0, %s($fp)", memOff)) //load到t0
                        .append("\n\t")
                        .append(String.format("sw\t\t$t0, %s($sp)", off)); //t0 save到sp
                continue;
            }

            if (arg.getNumber().isPresent()) {
                Integer value = arg.getNumber().get();
                //需要将value保存下来（保存到t0）
                sb.append("\n\t").append("li\t\t$t0, ").append(value);
                sb.append("\n\t").append("sw\t\t$t0, ").append(off).append("($sp)");
            } else {
                //肯定有，先load出来，再存进去
                Integer memOff = getMipsRegisterAllocator().getFpMemOff(arg.getName());
                sb
                        .append("\n\t")
                        .append(String.format("lw\t\t$t0, %s($fp)", memOff)) //load到t0
                        .append("\n\t")
                        .append(String.format("sw\t\t$t0, %s($sp)", off)); //t0 save到sp
            }
        }
        //最后调用完函数之后，恢复现场。
        sb
                .append("\n\t")
                .append("jal\t\t")
                .append(funcName)
                .append("\n\t")
                .append("lw\t\t$fp, ").append("0($sp)") //fp拿出来，复原到调用前
                .append("\n\t")
                .append("lw\t\t$ra, ").append("4($sp)") //ra拿出来，复原到调用前
                .append("\n\t")
                .append("addi\t$sp, $sp, 8"); //sp复原到调用前

        if (receiver != null) {
            if (getMipsRegisterAllocator().getFpMemOff(receiver.getName()) != null) {
                sb
                        .append("\n\t")
                        .append("lw\t\t$v0, ").append(getMipsRegisterAllocator().getFpMemOff(receiver.getName())).append("($fp)");
                return sb.toString();
            }

            sb.append("\n\t").append("addiu\t$sp, $sp, -4");
            //call是存值
            getMipsRegisterAllocator().addFpOffset(receiver.getName());
            sb.append("\n\t").append("sw\t\t$v0, ($sp)"); //保存返回值
        }

        return sb.toString();
    }
}
