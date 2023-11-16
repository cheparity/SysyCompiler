package middleEnd.llvm.visitor;

import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import middleEnd.ASTNodeVisitor;
import middleEnd.llvm.ir.BasicBlock;
import middleEnd.llvm.ir.Function;
import middleEnd.llvm.ir.IrBuilder;

public final class BlockVisitor implements ASTNodeVisitor {
    private final Function function;
    private final IrBuilder builder;

    public BlockVisitor(Function function, IrBuilder builder) {
        this.function = function;
        this.builder = builder;
    }

    @Override
    public void visit(ASTNode node) {
        assert node.getGrammarType().equals(GrammarType.BLOCK);
        BasicBlock basicBlock = builder.buildEntryBlock(function, node.getSymbolTable());
        //block -> {blockItem}
        for (var child : node.getChildren()) {
            child.accept(new LocalVarVisitor(basicBlock, builder));
            child.accept(new StmtVisitor(basicBlock, builder));
        }
    }
}
