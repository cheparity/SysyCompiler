package middleEnd.llvm.ir;

public final class NestBlock extends BasicBlock {
    private final BasicBlock fatherBlock;

    NestBlock(String name, BasicBlock fatherBlock) {
        super(name);
        fatherBlock.addNestBlock(this);
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
