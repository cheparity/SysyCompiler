package middleEnd.llvm.visitor;

import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import middleEnd.ASTNodeVisitor;
import middleEnd.llvm.ir.BasicBlock;
import middleEnd.llvm.ir.IrBuilder;

/**
 * blockVisitor<font color='red'>不负责建立基本块</font>，只负责把visit的blockItem加入到给定的基本块中。
 * <p>
 * 如果需要visit匿名块，或者需要新建基本块，请在调用blockVisitor之前自行指定新块。
 */
public final class BlockVisitor implements ASTNodeVisitor {
    private final IrBuilder builder;
    /**
     * basicBlock可能会在{@link StmtVisitor}中，由于if/for语句的存在，被改变。
     */
    private BasicBlock basicBlock;

    /**
     * @param basicBlock 需要visit的块
     * @param builder    IrBuilder
     */
    public BlockVisitor(BasicBlock basicBlock, IrBuilder builder) {
        this.basicBlock = basicBlock;
        this.builder = builder;
    }

    public IrBuilder getBuilder() {
        return builder;
    }

    public BasicBlock getBasicBlock() {
        return basicBlock;
    }

    public void updateVisitingBlock(BasicBlock basicBlock) {
        this.basicBlock = basicBlock;
    }

    @Override
    public void visit(ASTNode block) {
        assert block.getGrammarType().equals(GrammarType.BLOCK);
        basicBlock.setSymbolTable(block.getSymbolTable());
        //block -> {blockItem}
        for (var child : block.getChildren()) {
            if (child.getGrammarType() != GrammarType.BLOCK_ITEM) continue;
            child.accept(new LocalVarVisitor(this));
            child.accept(new StmtVisitor(this));
        }
    }

}
