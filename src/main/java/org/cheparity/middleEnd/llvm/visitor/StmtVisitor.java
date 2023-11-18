package middleEnd.llvm.visitor;

import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import middleEnd.ASTNodeVisitor;
import middleEnd.llvm.ir.BasicBlock;
import middleEnd.llvm.ir.IrBuilder;

public final class StmtVisitor implements ASTNodeVisitor {
    private final BasicBlock basicBlock;
    private final IrBuilder builder;

    public StmtVisitor(BasicBlock basicBlock, IrBuilder builder) {
        this.basicBlock = basicBlock;
        this.builder = builder;
    }

    /**
     * Stmt ->
     * <p>
     * LVal '=' Exp ';'
     * <p>
     * | LVal '=' 'getint''('')'';'
     * <p>
     * | [Exp] ';'
     * <p>
     * | Block
     * <p>
     * | 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
     * <p>
     * | 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
     * <p>
     * | 'break' ';'
     * <p>
     * | 'continue' ';'
     * <p>
     * | 'return' [Exp] ';'
     * <p>
     * | 'printf''('FormatString{','Exp}')'';'
     */
    @Override
    public void visit(ASTNode stmt) {
        //stmt -> 'return' Exp ';'
        if (stmt.getChild(0).getGrammarType() == GrammarType.RETURN) {
            if (stmt.getChildren().size() == 2) {
                //没有exp的情况，直接build空返回语句。
                builder.buildVoidRetInst(basicBlock);
            }
            var res = new IrUtil(builder, basicBlock).calc(stmt.getChild(1));
            if (res.isNum) builder.buildRetInstOfConst(basicBlock, res.getNumber());
            else builder.buildRetInstOfConst(basicBlock, res.getVariable());
        }
        //Stmt -> Block
        else if (stmt.getChild(0).getGrammarType() == GrammarType.BLOCK) {
            //语法检查阶段时已经确保语法没问题了，即不会出现引用块内变量的情况。
            stmt.accept(new BlockVisitor(basicBlock, builder));
        }
    }
}
