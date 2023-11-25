package middleEnd.llvm.visitor;

import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import middleEnd.ASTNodeVisitor;
import middleEnd.llvm.ir.EntryBlock;
import middleEnd.llvm.ir.Function;
import middleEnd.llvm.ir.IrBuilder;

public final class EntryBlockVisitor implements ASTNodeVisitor {
    private final Function function;
    private final IrBuilder builder;

    /**
     * 当block属于一个函数时，调用此构造方法。
     *
     * @param function 所属函数
     * @param builder  所属函数的IrBuilder
     */
    public EntryBlockVisitor(Function function, IrBuilder builder) {
        this.function = function;
        this.builder = builder;
    }


    @Override
    public void visit(ASTNode block) {
        assert block.getGrammarType().equals(GrammarType.BLOCK);
        EntryBlock entryBlock = builder.buildEntryBlock(function, block.getSymbolTable());
        //block -> {blockItem}
        for (var child : block.getChildren()) {
            if (child.getGrammarType() != GrammarType.BLOCK_ITEM) continue;
            child.accept(new LocalVarVisitor(entryBlock, builder));
            child.accept(new StmtVisitor(entryBlock, builder));
        }
    }
}
