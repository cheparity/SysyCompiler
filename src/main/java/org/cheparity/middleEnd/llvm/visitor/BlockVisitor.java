package middleEnd.llvm.visitor;

import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import middleEnd.ASTNodeVisitor;
import middleEnd.llvm.ir.BasicBlock;
import middleEnd.llvm.ir.Function;
import middleEnd.llvm.ir.IrBuilder;

public final class BlockVisitor implements ASTNodeVisitor {
    private final BasicBlock fatherBlock; //与下面的function不可共存！
    private final Function function;
    private final IrBuilder builder;
    /**
     * 区分是否是visit一个嵌入块。
     */
    private final boolean nested;

    /**
     * 当block属于一个函数时，调用此构造方法。
     *
     * @param function 所属函数
     * @param builder  所属函数的IrBuilder
     */
    public BlockVisitor(Function function, IrBuilder builder) {
        this.function = function;
        this.builder = builder;
        nested = false;
        this.fatherBlock = null;
    }


    /**
     * 如果是<font color='red'>嵌套block而不是函数的entry block</font>，则调用此构造方法。
     *
     * @param nestBlock 嵌套block
     * @param builder   IrBuilder
     */
    public BlockVisitor(BasicBlock nestBlock, IrBuilder builder) {
        this.function = nestBlock.getEntryFunc();
        nested = true;
        this.fatherBlock = nestBlock;
        this.builder = builder;
    }

//    /**
//     * 当block<font color='red'>是另一个block的子块时</font>，调用此构造方法。
//     *
//     * @param fatherBlock 所属块
//     * @param builder     父级块的构造方法
//     */
//    public BlockVisitor(BasicBlock fatherBlock, IrBuilder builder) {
//        this.function = null;
//        this.builder = builder;
//    }

    @Override
    public void visit(ASTNode block) {
        assert block.getGrammarType().equals(GrammarType.BLOCK);
        BasicBlock basicBlock;
        if (nested) { //如果是嵌入块，就不应该构造函数的entry block
            basicBlock = builder.buildNestBlock(fatherBlock, block.getSymbolTable());
        } else { //visit入口块
            basicBlock = builder.buildEntryBlock(function, block.getSymbolTable());
        }
        //block -> {blockItem}
        for (var child : block.getChildren()) {
            if (child.getGrammarType() != GrammarType.BLOCK_ITEM) continue;
            child.accept(new LocalVarVisitor(basicBlock, builder));
            child.accept(new StmtVisitor(basicBlock, builder));
        }
    }
}
