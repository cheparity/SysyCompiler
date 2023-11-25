package middleEnd.llvm.visitor;

import frontEnd.parser.dataStruct.ASTNode;
import middleEnd.ASTNodeVisitor;
import middleEnd.llvm.ir.BasicBlock;
import middleEnd.llvm.ir.IrBuilder;

public class BlockVisitor implements ASTNodeVisitor {
    private final BasicBlock basicBlock;
    private final IrBuilder builder;

    public BlockVisitor(BasicBlock basicBlock, IrBuilder builder) {
        this.basicBlock = basicBlock;
        this.builder = builder;
    }

    @Override
    public void visit(ASTNode node) {

    }
}
