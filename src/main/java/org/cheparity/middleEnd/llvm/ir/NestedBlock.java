package middleEnd.llvm.ir;

public final class NestedBlock extends BasicBlock {
    private final BasicBlock fatherBlock;
    private final boolean anonymous;
    private boolean firstInst = true;

    /**
     * 构建一个匿名嵌套块，解决块中块的问题，区别于if for等语句中的块。
     *
     * @param fatherBlock 父块。子块自动命名为父块名+nested。
     */
    NestedBlock(BasicBlock fatherBlock) {
        super(fatherBlock.getName() + "nested");
        this.fatherBlock = fatherBlock;
        fatherBlock.nestedBlocks.add(this);
        this.anonymous = true;
    }

    /**
     * 构建一个非匿名嵌套块，解决块中块的问题，区别于if for等语句中的块。
     *
     * @param label       块名
     * @param fatherBlock 父块
     */
    NestedBlock(String label, BasicBlock fatherBlock) {
        super(label);
        this.fatherBlock = fatherBlock;
        fatherBlock.nestedBlocks.add(this);
        this.anonymous = false;
    }

    /**
     * 递归把自己的指令添加到所有父块中。
     *
     * @param instruction 要添加的指令
     */
    @Override
    void addInstruction(Instruction instruction) {
        super.addInstruction(instruction);
        if (!anonymous && firstInst) {
//            fatherBlock.addInstruction(new Instruction("br label %" + getName()));
            firstInst = false;
        }
        fatherBlock.addInstruction(instruction);
    }
}
