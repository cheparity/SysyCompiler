package middleEnd.llvm.visitor;

import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.symbols.SymbolTable;
import middleEnd.ASTNodeVisitor;
import middleEnd.llvm.IrUtil;
import middleEnd.llvm.ir.BasicBlock;
import middleEnd.llvm.ir.IrBuilder;

public final class StmtVisitor implements ASTNodeVisitor {
    private final BasicBlock basicBlock;
    private final IrBuilder builder;
    private SymbolTable symbolTable = SymbolTable.getGlobal();

    public StmtVisitor(BasicBlock basicBlock, IrBuilder builder) {
        this.basicBlock = basicBlock;
        this.builder = builder;
    }

    @Override
    public void visit(ASTNode stmt) {
        //stmt -> 'return' [Exp] ';'
        //todo 这个SymbolTable很可能有问题
        var res = IrUtil.CalculateConst(stmt.getChild(1), symbolTable);
        builder.buildRetInstOfConst(basicBlock, res);
    }
}
