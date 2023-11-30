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
    private final BasicBlock basicBlock;
    private BlockVisitor callee;

    public StmtVisitor(BlockVisitor callee) {
        this.callee = callee;
        this.basicBlock = callee.getBasicBlock();
        this.builder = callee.getBuilder();
        this.symbolTable = callee.getBasicBlock().getSymbolTable();
    }

    private StmtVisitor(BasicBlock basicBlock, IrBuilder builder) {
        this.basicBlock = basicBlock;
        this.builder = builder;
        this.symbolTable = basicBlock.getSymbolTable();
    }


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
        //Stmt -> 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
        else if (stmt.getChild(0).getGrammarType() == GrammarType.FOR) {
            visitForStmt(stmt);
        }
        //Stmt -> Block
        else if (stmt.getChild(0).getGrammarType() == GrammarType.BLOCK) {
            stmt.accept(new BlockVisitor(basicBlock, builder)); //这自动就处理匿名内部类的问题了
        }
        //Stmt -> 'break' ';'
        else if (stmt.getChild(0).getGrammarType() == GrammarType.BREAK) {
            visitBreakStmt(stmt);
        }
        //Stmt -> 'continue' ';'
        else if (stmt.getChild(0).getGrammarType() == GrammarType.CONTINUE) {
            visitContinueStmt(stmt);
        }
    }

    private void visitBreakStmt(ASTNode breakStmt) {
        //breakStmt所在块肯定是loopStmt，loopStmt的前驱是Cond
        //break就是要跳到Cond的后继--finalBlk
        // todo 但是此时finalBlk还没有建立！
        /*
         * 我希望：
         * pauseUntilNotified();
         * 然后再执行下面的语句
         */
        var finalBlk = basicBlock.findPreWithTag("cond").findSucWithTag("forEnd");
        assert finalBlk != null;
        builder.buildBrInst(basicBlock, finalBlk);

    }


    private void visitContinueStmt(ASTNode continueStmt) {
        //continueStmt所在块肯定是loopStmt，loopStmt的后继就是forStmt2
        //continue就是要跳到Cond的后继--forStmt2Blk
        BasicBlock forStmt2Blk = basicBlock.findPreWithTag("forBody").findSucWithTag("forStmt2");
        assert forStmt2Blk != null;
        builder.buildBrInst(basicBlock, forStmt2Blk);
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
        final ASTNode ifTrueNodeStmt = ifStmt.getChild(4);

        //如果cond可以直接判断，则没必要构建if的结构
        NodeUnion condUnion = new IrUtil(builder, basicBlock).calcLogicExp(condNode);
        if (condUnion.isNum) {
            int number = condUnion.getNumber();
            LOGGER.fine("Meet [ " + number + " ]. Jump to ifTrueBlk or elseBlk directly!");
            //如果是1，则直接接着解析ifTrueBlk里的东西；如果是0且有else，直接建立ElseBlk，如果是0且无else，直接跳过（return）
            //这里wrap与否应该差不多
            if (number == 1) {
                ifTrueNodeStmt.accept(new StmtVisitor(basicBlock, builder));
                return;
            }
            if (hasElseStmt) {
                var elseStmt = ifStmt.getChild(-1);
                elseStmt.accept(new StmtVisitor(basicBlock, builder));
            }
            return;
        }
        //如果cond判断不了，则构建if的结构
        //处理各个block
        Variable cond = condUnion.getVariable();
        //需要把stmt包装为block => 需要新建一个block
        ifTrueBlk = builder.buildBasicBlock(basicBlock).setTag("ifTrue");
        ifTrueNodeStmt.accept(new StmtVisitor(ifTrueBlk, builder));
        if (hasElseStmt) {
//            ASTNode elseStmt = IrUtil.wrapStmtAsBlock(ifStmt.getChild(-1), symbolTable);
            var elseStmt = ifStmt.getChild(-1);
            elseBlk = builder.buildBasicBlock(basicBlock).setTag("else");
            elseStmt.accept(new StmtVisitor(elseBlk, builder));
            //需要给elseBlk增加br语句
        }
        finalBlk = builder.buildBasicBlock(basicBlock, basicBlock.getSymbolTable()).setTag("ifEnd");//新建一个基本块

        //在全部解析完ifTrueBlk, finalBlk, elseBlk之后，才可以buildBrInst
        if (elseBlk != null) {
            builder.buildBrInst(basicBlock, cond, ifTrueBlk, elseBlk); //entryBlock 根据 条件 -> ifTrueBlk | elseBlk
            builder.buildBrInst(elseBlk, finalBlk); //elseBlk -> finalBlk
        } else {
            builder.buildBrInst(basicBlock, cond, ifTrueBlk, finalBlk); //entryBlock 根据 条件 -> ifTrueBlk | finalBlk
            builder.buildBrInst(ifTrueBlk, finalBlk); //ifTrueBlk -> finalBlk
        }
        //这里应该是调用者的basicBlock = finalBlock？
        // 其实可以通过传递callee的方式传递过来
        callee.updateVisitingBlock(finalBlk);
    }

    //Stmt -> 'for' '(' [ForStmt1] ';' [Cond] ';' [ForStmt2] ')' Stmt
    private void visitForStmt(ASTNode forStmt) {
        //找到两个;的位置，根据;的位置来判断是否有ForStmt
        int semi1 = 0, semi2 = 0;
        for (int i = 2; i < forStmt.getChildren().size(); i++) {
            var child = forStmt.getChild(i);
            if (child.getGrammarType() != GrammarType.SEMICOLON) continue;
            if (semi1 == 0) semi1 = i;
            else semi2 = i;
        }
        var forStmt1 = semi1 == 2 ? null : forStmt.getChild(2);
        var cond = semi2 - semi1 == 1 ? null : forStmt.getChild(semi1 + 1);
        var forStmt2 = semi2 == forStmt.getChildren().size() - 2 ? null : forStmt.getChild(semi2 + 1);
        var loopStmt = forStmt.getChild(-1);
        LOGGER.info("get forStmt1: " + forStmt1 + " cond: " + cond + " forStmt2: " + forStmt2);

        //我们需要建立的块有：forStmt1，Cond，ForStmt2，Stmt，Stmt之后的语句。
        //1.执行初始化表达式ForStmt1 => 可以放在if所处的block内
        if (forStmt1 != null) {
            // ForStmt -> LVal '=' Exp，是Stmt的一种特殊情况
            forStmt1.accept(new StmtVisitor(basicBlock, builder));
        }

        BasicBlock condBlk = builder.buildBasicBlock(basicBlock).setTag("cond");
        NodeUnion condUnion;
        if (cond == null) {
            condUnion = new NodeUnion(null, builder, condBlk.setSymbolTable(symbolTable))
                    .setNumber(1);
        } else {
            condUnion = new IrUtil(builder, condBlk.setSymbolTable(symbolTable)).calcLogicExp(cond);
        }
        if (condUnion.isNum && condUnion.getNumber() == 0) {
            //如果恒为0则啥也不用干。需要把condBlk drop掉
            basicBlock.dropBlock(condBlk);
            return;
        }

        //处理loop循环体
        var loopBlk = builder.buildBasicBlock(condBlk, symbolTable).setTag("forBody");
        //todo 为什么要单独拿出来呢？因为后续可能会回填continue和break语句
        StmtVisitor loopStmtVisitor = new StmtVisitor(loopBlk, builder);
        loopStmt.accept(loopStmtVisitor);
        BasicBlock forStmt2Blk = null;
        if (forStmt2 != null) {
            forStmt2Blk = builder.buildBasicBlock(loopBlk, symbolTable).setTag("forStmt2");
            forStmt2.accept(new StmtVisitor(forStmt2Blk, builder));
        }

        var finalBlk = builder.buildBasicBlock(condBlk, basicBlock.getSymbolTable()).setTag("forEnd");
        //新建一个基本块

        //开始build br inst
        if (condUnion.isNum && condUnion.getNumber() == 1) {
            //如果恒为1，则直接跳转到loopBlk
            builder.buildBrInst(condBlk, loopBlk);
        } else {
            builder.buildBrInst(condBlk, condUnion.getVariable(), loopBlk, finalBlk);
        }
        builder.buildBrInst(basicBlock, condBlk);
        //从loop跳转到forStmt2Blk
        if (forStmt2Blk != null) {
            builder.buildBrInst(loopBlk, forStmt2Blk);
            builder.buildBrInst(forStmt2Blk, condBlk);
        } else {
            builder.buildBrInst(loopBlk, condBlk);
        }
        //最后把callee的basicBlock设为finalBlk
        callee.updateVisitingBlock(finalBlk);
    }

}
