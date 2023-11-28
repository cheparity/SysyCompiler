package middleEnd.llvm.visitor;

import frontEnd.parser.dataStruct.ASTLeaf;
import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import middleEnd.ASTNodeVisitor;
import middleEnd.llvm.ir.BasicBlock;
import middleEnd.llvm.ir.IrBuilder;
import middleEnd.llvm.ir.PointerValue;
import middleEnd.llvm.ir.Variable;
import middleEnd.llvm.utils.NodeUnion;
import middleEnd.symbols.Symbol;
import middleEnd.symbols.SymbolTable;
import utils.LoggerUtil;

import java.util.List;
import java.util.logging.Logger;

public final class StmtVisitor implements ASTNodeVisitor {
    private static final Logger LOGGER = LoggerUtil.getLogger();
    final IrBuilder builder;
    final SymbolTable symbolTable;
    BlockVisitor callee;
    BasicBlock basicBlock;

    public StmtVisitor(BlockVisitor callee) {
        this.callee = callee;
        this.basicBlock = callee.getBasicBlock();
        this.builder = callee.getBuilder();
        this.symbolTable = callee.getBasicBlock().getSymbolTable();
    }

    public StmtVisitor(BasicBlock basicBlock, IrBuilder builder) {
        this.basicBlock = basicBlock;
        this.builder = builder;
        this.symbolTable = basicBlock.getSymbolTable();
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
            visitRetStmt(stmt);
        }
        //Stmt -> LVal '=' 'getint''('')'';'
        else if (stmt.deepDownFind(GrammarType.GETINT, 1).isPresent()) {
            visitGetintStmt(stmt);
        }
        //stmt -> 'printf''('FormatString{','Exp}')'';'
        else if (stmt.getChild(0).getGrammarType() == GrammarType.PRINTF) {
            visitPrintfStmt(stmt);
        }
        //Stmt -> LVal '=' Exp ';'
        else if (stmt.getChild(0).getGrammarType() == GrammarType.LVAL) {
            visitLvalStmt(stmt);
        }
        //Stmt -> Exp ';'
        else if (stmt.getChild(0).getGrammarType() == GrammarType.EXP) {
            new IrUtil(builder, basicBlock).calcAloExp(stmt.getChild(0));
        }
        //Stmt -> 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
        else if (stmt.getChild(0).getGrammarType() == GrammarType.IF) {
            visitIfStmt(stmt);
        }
        //Stmt -> Block
        else if (stmt.getChild(0).getGrammarType() == GrammarType.BLOCK) {
            //这里只处理匿名块的情况
            GrammarType brotherGraTy = stmt.getFather().getChild(0).getGrammarType();
            if (brotherGraTy != GrammarType.IF && brotherGraTy != GrammarType.FOR) {
                visitAnonymousBlock(stmt);
            } else {
                throw new RuntimeException("Shouldn't visit anonymous block here! Please check if IF or FOR error.");
            }
        }
    }


    private void visitAnonymousBlock(ASTNode blockStmt) {
        // 匿名块。
        ASTNode anonymousBlk = blockStmt.getChild(0).getGrammarType() == GrammarType.BLOCK ? blockStmt.getChild(0) : blockStmt;
        assert anonymousBlk.getGrammarType() == GrammarType.BLOCK;
        anonymousBlk.getChildren().stream().filter(child -> child.getGrammarType() == GrammarType.BLOCK_ITEM).forEach(blkItm -> {
            SymbolTable blockST = anonymousBlk.getSymbolTable();
            assert blockST != null;
            // 注意调用的构造函数是不一样的
            blkItm.accept(new LocalVarVisitor(basicBlock.setSymbolTable(blockST), builder));
            blkItm.accept(new StmtVisitor(basicBlock.setSymbolTable(blockST), builder));
        });
    }

    private void visitLvalStmt(ASTNode lvalStmt) {
        //LVal -> Ident {'[' Exp ']'}
        assert symbolTable.getSymbol(lvalStmt.getChild(0).getRawValue()).isPresent();
        Symbol symbol = symbolTable.getSymbol(lvalStmt.getChild(0).getRawValue()).get();
        PointerValue pointer = symbol.getPointer(); //a:%1
        assert pointer != null;
        NodeUnion result = new IrUtil(builder, basicBlock).calcAloExp(lvalStmt.getChild(2));
        if (result.isNum) {
            builder.buildStoreInst(basicBlock, builder.buildConstIntNum(result.getNumber()), pointer);
        } else {
            builder.buildStoreInst(basicBlock, result.getVariable(), pointer);
        }
    }

    private void visitPrintfStmt(ASTNode printStmt) {
        var formatString = ((ASTLeaf) printStmt.getChild(2)).getToken().getRawValue();
        List<ASTNode> exps = printStmt.getChildren().stream().filter(node -> node.getGrammarType() == GrammarType.EXP).toList();
        var args = new Variable[exps.size()];
        for (int i = 0; i < exps.size(); i++) {
            var res = new IrUtil(builder, basicBlock).calcAloExp(exps.get(i));
            if (res.isNum) args[i] = builder.buildConstIntNum(res.getNumber());
            else args[i] = res.getVariable();
        }
        //解析formatString
        char[] chars = formatString.toCharArray();
        int argCnt = 0;
        for (int i = 1; i < chars.length - 1; i++) {
            if (chars[i] == '%' && chars[i + 1] == 'd') {
                builder.buildCallInst(basicBlock, "putint", args[argCnt]);
                argCnt++;
                i++;
            } else if (chars[i] == '\\' && chars[i + 1] == 'n') {
                builder.buildCallInst(basicBlock, "putch", builder.buildConstIntNum(10));
                i++;
            } else {
                builder.buildCallInst(basicBlock, "putch", builder.buildConstIntNum(chars[i]));
            }
        }
    }

    private void visitGetintStmt(ASTNode getintStmt) {
        var lval = getintStmt.getChild(0);
        String lvalName = lval.getChild(0).getRawValue();
        assert basicBlock.getSymbolTable().getSymbol(lvalName).isPresent();
        Symbol symbol = basicBlock.getSymbolTable().getSymbol(lvalName).get();
        //给symbol重新赋值
        Variable variable = builder.buildCallInst(basicBlock, "getint");
        builder.buildStoreInst(basicBlock, variable, symbol.getPointer());
    }

    private void visitRetStmt(ASTNode retStmt) {
        //如果没有return，还要补上，否则过不了llvm的编译。那就默认ret void / ret i32 0（不在这里做，在Function里做）
        if (retStmt.getChildren().size() == 2) {
            //没有exp的情况，直接build空返回语句。
            builder.buildVoidRetInst(basicBlock);
            return;
        }
        var res = new IrUtil(builder, basicBlock).calcAloExp(retStmt.getChild(1));
        if (res.isNum) builder.buildRetInstOfConst(basicBlock, res.getNumber());
        else builder.buildRetInstOfConst(basicBlock, res.getVariable());
    }

    //Stmt -> 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
    private void visitIfStmt(ASTNode ifStmt) {
        //Cond -> LOrExp
        //LOrExp -> LAndExp | LOrExp '||' LAndExp
        //LAndExp -> EqExp | LAndExp '&&' EqExp
        //EqExp -> RelExp | EqExp ('==' | '!=') RelExp
        //RelExp -> AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
        BasicBlock ifTrueBlk, finalBlk, elseBlk = null;
        final boolean hasElseStmt = ifStmt.deepDownFind(GrammarType.ELSE, 1).isPresent();
        final ASTNode condNode = ifStmt.getChild(2);
        final ASTNode ifTrueNodeStmt = IrUtil.wrapStmtAsBlock(ifStmt.getChild(4), symbolTable);
        //处理Cond
        NodeUnion union = new IrUtil(builder, basicBlock).calcLogicExp(condNode);
        if (union.isNum) {
            int number = union.getNumber();
            LOGGER.fine("Meet [ " + number + " ]. Jump to ifTrueBlk or elseBlk directly!");
            //如果是1，则直接接着解析ifTrueBlk里的东西；如果是0且有else，直接建立ElseBlk，如果是0且无else，直接跳过（return）
            if (number == 1) {
                visitAnonymousBlock(ifTrueNodeStmt);
                return;
            }
            if (hasElseStmt) {
                var elseStmt = ifStmt.getChild(-1);
                visitAnonymousBlock(elseStmt);
            }
            return;
        }
        //处理各个block
        Variable cond = union.getVariable();
        //需要把stmt包装为block => 需要新建一个block
        ifTrueBlk = visitControlFlowBlock(ifTrueNodeStmt);
        if (hasElseStmt) {
            ASTNode elseStmt = IrUtil.wrapStmtAsBlock(ifStmt.getChild(-1), symbolTable);
            elseBlk = visitControlFlowBlock(elseStmt);
            //需要给elseBlk增加br语句
        }
        finalBlk = builder.buildBasicBlock(basicBlock, basicBlock.getSymbolTable());//新建一个基本块

        //在全部解析完ifTrueBlk, finalBlk, elseBlk之后，才可以buildBrInst
        if (elseBlk != null) {
            builder.buildBrInst(basicBlock, cond, ifTrueBlk, elseBlk); //entryBlock 根据 条件 -> ifTrueBlk | elseBlk
            builder.buildBrInst(elseBlk, finalBlk); //elseBlk -> finalBlk
        } else {
            builder.buildBrInst(basicBlock, cond, ifTrueBlk, finalBlk); //entryBlock 根据 条件 -> ifTrueBlk | finalBlk
            assert ifTrueBlk != null;
            builder.buildBrInst(ifTrueBlk, finalBlk); //ifTrueBlk -> finalBlk
        }
        //这里应该是调用者的basicBlock = finalBlock？
        // 其实可以通过传递callee的方式传递过来
        callee.setBasicBlock(finalBlk);
    }

    /**
     * 确保只被visitIfStmt调用！
     *
     * @param blockStmt 控制流块
     * @return 匿名块对应的基本块
     */
    private BasicBlock visitControlFlowBlock(ASTNode blockStmt) {
        BlockVisitor visitor = new BlockVisitor(basicBlock, builder);
        blockStmt.accept(visitor);
        return visitor.getBasicBlock();
    }


}
