package middleEnd.llvm.visitor;

import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import middleEnd.ASTNodeVisitor;
import middleEnd.llvm.ir.BasicBlock;
import middleEnd.llvm.ir.IrBuilder;

public final class BlockVisitor implements ASTNodeVisitor {
    private final BasicBlock basicBlock; //与下面的function不可共存！
    private final IrBuilder builder;

    public BlockVisitor(BasicBlock basicBlock, IrBuilder builder) {
        this.basicBlock = basicBlock;
        this.builder = builder;
    }


    @Override
    public void visit(ASTNode block) {
        assert block.getGrammarType().equals(GrammarType.BLOCK);
        basicBlock.setSymbolTable(block.getSymbolTable());
        //block -> {blockItem}
        for (var child : block.getChildren()) {
            if (child.getGrammarType() != GrammarType.BLOCK_ITEM) continue;
            child.accept(new LocalVarVisitor(basicBlock, builder));
            child.accept(new StmtVisitor(basicBlock, builder));
        }
    }
}
