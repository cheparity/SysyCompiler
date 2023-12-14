package middleEnd.llvm.ir;

import java.util.concurrent.atomic.AtomicReference;

/**
 * br i1 [cond], label [iftrue], label [iffalse]
 * <p>
 * br label [dest]          ; Unconditional branch
 */
public final class BrInstruction extends Instruction {
    final boolean isConditional;

    Variable cond;
    AtomicReference<BasicBlock> ifTrue;
    AtomicReference<BasicBlock> ifFalse;
    AtomicReference<BasicBlock> dest;

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
        this.ifTrue = new AtomicReference<>(ifTrue);
        this.ifFalse = new AtomicReference<>(ifFalse);
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
        this.dest = new AtomicReference<>(dest);
    }

    @Override
    public String toIrCode() {
        if (isConditional) {
            assert cond != null;
            assert ifTrue != null;
            assert ifFalse != null;
            return String.format("br i1 %s, label %s, label %s", cond.toIrCode(), ifTrue.get().getName(),
                    ifFalse.get().getName());
        } else {
            assert dest != null;
            return String.format("br label %s", dest.get().getName());
        }
    }
}
