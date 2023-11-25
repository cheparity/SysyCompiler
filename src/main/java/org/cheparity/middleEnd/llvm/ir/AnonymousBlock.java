package middleEnd.llvm.ir;

public final class AnonymousBlock extends BasicBlock {
    private final BasicBlock fatherBlock;

    /**
     * 构建一个匿名嵌套块，解决块中块的问题，区别于if for等语句中的块。
     *
     * @param fatherBlock 父块。子块自动命名为父块名+nested。
     */
    AnonymousBlock(BasicBlock fatherBlock) {
        super(fatherBlock.getName() + "nested");
        this.fatherBlock = fatherBlock;
    }


    /**
     * 递归把自己的指令添加到所有父块中。
     *
     * @param instruction 要添加的指令
     */
    @Override
    void addInstruction(Instruction instruction) {
        super.addInstruction(instruction);
        fatherBlock.addInstruction(instruction);
    }
}
