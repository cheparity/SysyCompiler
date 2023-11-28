package middleEnd.llvm.visitor;

import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import middleEnd.ASTNodeVisitor;
import middleEnd.llvm.ir.BasicBlock;
import middleEnd.llvm.ir.Function;
import middleEnd.llvm.ir.IrBuilder;

public final class BlockVisitor implements ASTNodeVisitor {
    private final IrBuilder builder;
    /**
     * basicBlock可能会在{@link StmtVisitor}中，由于if/for语句的存在，被改变。
     */
    private BasicBlock basicBlock;

    /**
     * 只在{@link StmtVisitor}里被调用
     *
     * @param predecessor 前驱基本块
     * @param builder     IrBuilder
     */
    public BlockVisitor(BasicBlock predecessor, IrBuilder builder) {
        this.basicBlock = builder.buildBasicBlock(predecessor);
        this.builder = builder;
    }

    /**
     * 只在{@link FuncVisitor}里被调用
     *
     * @param function 入口函数
     * @param builder  IrBuilder
     */
    public BlockVisitor(Function function, IrBuilder builder) {
        this.basicBlock = builder.buildEntryBlock(function);
        this.builder = builder;
    }

    public IrBuilder getBuilder() {
        return builder;
    }

    public BasicBlock getBasicBlock() {
        return basicBlock;
    }

    public void setBasicBlock(BasicBlock basicBlock) {
        this.basicBlock = basicBlock;
    }

    @Override
    public void visit(ASTNode block) {
        assert block.getGrammarType().equals(GrammarType.BLOCK);
//        basicBlock.getFunction().addBlock(basicBlock);
        basicBlock.setSymbolTable(block.getSymbolTable());
        //block -> {blockItem}
        for (var child : block.getChildren()) {
            if (child.getGrammarType() != GrammarType.BLOCK_ITEM) continue;
            child.accept(new LocalVarVisitor(this));
            child.accept(new StmtVisitor(this));
        }
    }

}
