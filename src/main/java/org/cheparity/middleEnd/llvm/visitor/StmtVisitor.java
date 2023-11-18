package middleEnd.llvm.visitor;

import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import middleEnd.ASTNodeVisitor;
import middleEnd.llvm.ir.BasicBlock;
import middleEnd.llvm.ir.IrBuilder;
import middleEnd.llvm.ir.Variable;
import middleEnd.symbols.Symbol;

import java.util.List;

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
        //Stmt -> LVal '=' 'getint''('')'';'
        else if (stmt.deepDownFind(GrammarType.GETINT, 1).isPresent()) {
            var lval = stmt.getChild(0);
            String lvalName = lval.getChild(0).getRawValue();
            assert basicBlock.getSymbolTable().getSymbol(lvalName).isPresent();
            Symbol symbol = basicBlock.getSymbolTable().getSymbol(lvalName).get();
            //给symbol重新赋值
            Variable variable = builder.buildCallInst(basicBlock, "getint");
            builder.buildAssignInst(basicBlock, variable, symbol.getPointer());
        }
        //stmt -> 'printf''('FormatString{','Exp}')'';'
        else if (stmt.getChild(0).getGrammarType() == GrammarType.PRINTF) {
            var formatString = stmt.getChild(2).getRawValue();
            List<ASTNode> exps = stmt.getChildren().stream().filter(node -> node.getGrammarType() == GrammarType.EXP).toList();
            var args = new Variable[exps.size()];
            for (int i = 0; i < exps.size(); i++) {
                var res = new IrUtil(builder, basicBlock).calc(exps.get(i));
                if (res.isNum) args[i] = builder.buildConstIntNum(res.getNumber());
                else args[i] = res.getVariable();
            }
            //解析formatString
            char[] chars = formatString.toCharArray();
            int argCnt = 0;
            for (int i = 0; i < chars.length; i++) {
                if (chars[i] == '%' && chars[i + 1] == 'd') {
                    builder.buildCallInst(basicBlock, "putch", args[argCnt]);
                    argCnt++;
                    i++;
                } else {
                    builder.buildCallInst(basicBlock, "putch", builder.buildConstIntNum(chars[i]));
                }
            }
        }
    }
}
