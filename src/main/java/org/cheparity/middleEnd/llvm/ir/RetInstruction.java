package middleEnd.llvm.ir;

/**
 * ret [type] [value] ,ret void
 * <p>
 * 确实用到了value，后面应该不用重构
 */
public final class RetInstruction extends Instruction {
    private final Variable retValue;
    private final boolean retVoid;

    RetInstruction(Variable result) {
        this.retValue = result;
        retVoid = false;
    }

    RetInstruction() {
        retValue = null;
        retVoid = true;
    }

    @Override
    public String toIrCode() {
        //ret <return_type> <return_value>
        if (retVoid) {
            return "ret void";
        }
        assert retValue != null;
        return "ret " + retValue.getType().toIrCode() + " " + retValue.toIrCode();
    }

    @Override
    public String toMipsCode() {
        var sb = new StringBuilder();
        //准备返回值
        if (!retVoid) {
            assert retValue != null;
            if (retValue.getNumber().isPresent()) {
                sb.append(String.format("li\t\t$v0, %s\n\t", retValue.getNumber().get()));
            } else {
                Integer memOff = getMipsRegisterAllocator().getMemOff(retValue.getName());
                sb.append(String.format("lw\t\t$v0, %s($fp)\n\t", memOff));
            }

        }

        sb //sp指针回到栈顶
                .append("move\t$sp, $fp")
                .append("\n\t")
                .append("jr\t\t$ra\n\tnop\n");
        return sb.toString();
    }
}
