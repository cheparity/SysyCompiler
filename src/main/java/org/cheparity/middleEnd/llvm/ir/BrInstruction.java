package middleEnd.llvm.ir;

/**
 * br i1 [cond], label [iftrue], label [iffalse]
 * <p>
 * br label [dest]          ; Unconditional branch
 */
public final class BrInstruction extends Instruction {
    final boolean isConditional;

    Variable cond;
    BasicBlock ifTrue;
    BasicBlock ifFalse;
    BasicBlock dest;

    /**
     * Unconditional branch
     *
     * @param cond    条件，必须是i1类型
     * @param ifTrue  如果条件为真，跳转到的块
     * @param ifFalse 如果条件为假，跳转到的块
     */
    BrInstruction(Variable cond, BasicBlock ifTrue, BasicBlock ifFalse) {
        this.isConditional = true;
        this.cond = cond;
        this.ifTrue = ifTrue;
        this.ifFalse = ifFalse;
        this.dest = null;
    }

    /**
     * Unconditional branch
     *
     * @param dest 无条件跳转到的块
     */
    BrInstruction(BasicBlock dest) {
        this.isConditional = false;
        this.cond = null;
        this.ifTrue = null;
        this.ifFalse = null;
        this.dest = dest;
    }

    @Override
    public String toIrCode() {
        if (isConditional) {
            assert cond != null;
            assert ifTrue != null;
            assert ifFalse != null;
            return String.format("br i1 %s, label %s, label %s", cond.toIrCode(), ifTrue.getName(),
                    ifFalse.getName());
        } else {
            assert dest != null;
            return String.format("br label %s", dest.getName());
        }
    }
}
