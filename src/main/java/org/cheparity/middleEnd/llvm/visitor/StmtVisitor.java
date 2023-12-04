package middleEnd.llvm.visitor;

import frontEnd.parser.dataStruct.ASTLeaf;
import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import middleEnd.ASTNodeVisitor;
import middleEnd.llvm.BlockController;
import middleEnd.llvm.ir.BasicBlock;
import middleEnd.llvm.ir.IrBuilder;
import middleEnd.llvm.ir.PointerValue;
import middleEnd.llvm.ir.Variable;
import middleEnd.llvm.utils.NodeUnion;
import middleEnd.symbols.Symbol;
import middleEnd.symbols.SymbolTable;
import utils.CallBack;
import utils.LoggerUtil;
import utils.Message;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public final class StmtVisitor implements ASTNodeVisitor, BlockController {
    private static final Logger LOGGER = LoggerUtil.getLogger();
    final IrBuilder builder;
    final SymbolTable symbolTable;
    private final LinkedList<Message> messages = new LinkedList<>();
    private final ASTNodeVisitor caller;
    private BasicBlock basicBlock;

    public StmtVisitor(BlockVisitor caller) {
        this.caller = caller;
        this.basicBlock = caller.getBasicBlock();
        this.builder = caller.getBuilder();
        this.symbolTable = caller.getBasicBlock().getSymbolTable();
    }

    /**
     * StmtVisitor的caller只有可能是BlockVisitor或者StmtVisitor。这个函数是为了处理调用者是StmtVisitor的caller
     *
     * @param basicBlock 当前的基本块
     * @param caller     调用者
     */
    private StmtVisitor(BasicBlock basicBlock, ASTNodeVisitor caller) {
        this.caller = caller;
        this.basicBlock = basicBlock;
        this.builder = caller.getBuilder();
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
            stmt.accept(new BlockVisitor(basicBlock, this)); //这自动就处理匿名内部类的问题了
        }
        //Stmt -> 'break' ';'
        else if (stmt.getChild(0).getGrammarType() == GrammarType.BREAK) {
            visitBreakStmt();
        }
        //Stmt -> 'continue' ';'
        else if (stmt.getChild(0).getGrammarType() == GrammarType.CONTINUE) {
            visitContinueStmt();
        }
    }

    @Override
    public IrBuilder getBuilder() {
        return this.builder;
    }

    @Override
    public void emit(Message message, ASTNodeVisitor sender) {
        //这个就要做事情了
        //要过滤出来是getFinalBlk的消息
        if (sender == this) {
            LOGGER.info(this + " cannot handle the message, continue to pass.");
            caller.emit(message, sender);
            return;
        }
        if (catchMessage(message, "breakReq", "continueReq")) {
            return;
        }
        LOGGER.info(this + " cannot handle the message, continue to pass.");
        caller.emit(message, sender);
    }

    private boolean catchMessage(Message message, String... requests) {
        AtomicBoolean isCaught = new AtomicBoolean(false);
        Arrays.stream(requests).toList().forEach(request -> {
            if (message.request.equals(request)) {
                LOGGER.info(this + " catches message " + message.request + " , and continue" +
                        " to broadcast.");
                this.messages.add(message);
                caller.emit(message, this);
                isCaught.set(true);
            }
        });
        return isCaught.get();
    }

    private void visitBreakStmt() {
        //breakStmt所在块肯定是loopStmt，loopStmt的前驱是Cond
        //break就是要跳到Cond的后继--finalBlk
        var callBack = new CallBack<BasicBlock>() {
            @Override
            public void run(BasicBlock finalBlk) {
                assert finalBlk != null;
                builder.buildBrInst(basicBlock, finalBlk);
                LOGGER.info("execute callback of finalBlk " + finalBlk.getName());
            }
        };
        Message<CallBack<BasicBlock>> callBackMessage = new Message<>(callBack, "breakReq");
        LOGGER.info(this + " send the message of " + callBackMessage);
        caller.emit(callBackMessage, this);
    }

    private void visitContinueStmt() {
        //continueStmt所在块肯定是loopStmt，loopStmt的后继就是forStmt2
        //continue就是要跳到Cond的后继--forStmt2Blk
        var callBack = new CallBack<BasicBlock>() {
            @Override
            public void run(BasicBlock forStmt2) {
                assert forStmt2 != null;
                builder.buildBrInst(basicBlock, forStmt2);
                LOGGER.info("execute callback of forStmt2 " + forStmt2.getName());
            }
        };
        Message<CallBack<BasicBlock>> callBackMessage = new Message<>(callBack, "continueReq");
        LOGGER.info(this + " send the message of " + callBackMessage);
        caller.emit(callBackMessage, this);
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
                ifTrueNodeStmt.accept(new StmtVisitor(basicBlock, this));
                return;
            }
            if (hasElseStmt) {
                var elseStmt = ifStmt.getChild(-1);
                elseStmt.accept(new StmtVisitor(basicBlock, this));
            }
            return;
        }
        //如果cond判断不了，则构建if的结构
        //处理各个block
        Variable cond = condUnion.getVariable();
        //需要把stmt包装为block => 需要新建一个block
        ifTrueBlk = builder.buildBasicBlock(basicBlock).setTag("ifTrue");
        ifTrueNodeStmt.accept(new StmtVisitor(ifTrueBlk, this)); //此时如果是break或者continue，则还不知道，要visit完for stmt之后才能知道跳转到哪里
        //所以不用管
        if (hasElseStmt) {
//            ASTNode elseStmt = IrUtil.wrapStmtAsBlock(ifStmt.getChild(-1), symbolTable);
            var elseStmt = ifStmt.getChild(-1);
            elseBlk = builder.buildBasicBlock(basicBlock).setTag("else");
            elseStmt.accept(new StmtVisitor(elseBlk, this));
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
        // 其实可以通过传递caller的方式传递过来
        assert caller instanceof BlockController;
        ((BlockController) caller).updateVisitingBlk(finalBlk);
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
        var forStmt2 = semi2 == forStmt.getChildren().size() - 3 ? null : forStmt.getChild(semi2 + 1);
        var loopStmt = forStmt.getChild(-1);
        LOGGER.info("get forStmt1: " + forStmt1 + " cond: " + cond + " forStmt2: " + forStmt2);

        //我们需要建立的块有：forStmt1，Cond，ForStmt2，Stmt，Stmt之后的语句。
        //1.执行初始化表达式ForStmt1 => 可以放在if所处的block内
        if (forStmt1 != null) {
            // ForStmt -> LVal '=' Exp，是Stmt的一种特殊情况
            forStmt1.accept(new StmtVisitor(basicBlock, this));
        }

        BasicBlock condBlk, loopStartBlk, forStmt2Blk, finalBlk, loopEndBlk, beforeForBlk = basicBlock.setTag("beforeFor");
        //condBlk是无论如何都要创建的
        condBlk = builder.buildBasicBlock(basicBlock).setTag("cond");
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
        //此时basicBlock的意义还是beforeForBlk，即for语句所在的块
        loopStartBlk = builder.buildBasicBlock(condBlk, symbolTable).setTag("forBody");
        loopStmt.accept(new StmtVisitor(loopStartBlk, this));
        forStmt2Blk = null;
        if (forStmt2 != null) {
            forStmt2Blk = builder.buildBasicBlock(loopStartBlk, symbolTable).setTag("forStmt2");
            forStmt2.accept(new StmtVisitor(forStmt2Blk, this));
        }
        //此时basicBlock可能会改变，由原先的“for语句所在的块”，变为“loop语句的结束块”
        //当然如果没有改变的话，还是for语句所在的块。我们需要将其改为loop所在的块（loopStartBlk）作为“loop语句结束的块”
        if (basicBlock == beforeForBlk) {
            loopEndBlk = loopStartBlk.setTag("loopEnd");
        } else {
            loopEndBlk = basicBlock.setTag("loopEnd");
        }

        finalBlk = builder.buildBasicBlock(condBlk, beforeForBlk.getSymbolTable()).setTag("forEnd");
        LOGGER.info(this + " ready to handle [getFinalBlkCallBackList]");
        //新建一个基本块
        if (!this.messages.isEmpty()) {
            messages.stream().filter(message -> message.request.equals("breakReq")).toList().forEach(message -> {
                ((CallBack<BasicBlock>) message.data).run(finalBlk);
                messages.remove(message);
            });
            BasicBlock finalForStmt2Blk = forStmt2Blk;
            messages.stream().filter(message -> message.request.equals("continueReq")).toList().forEach(message -> {
                if (finalForStmt2Blk != null) {
                    ((CallBack<BasicBlock>) message.data).run(finalForStmt2Blk);
                } else { //如果是null，就直接continue到condBlk
                    ((CallBack<BasicBlock>) message.data).run(condBlk);
                }
                messages.remove(message);
            });
        }

        builder.buildBrInst(beforeForBlk, condBlk); //cond处理完了，basicBlock直接跳，因为后面basicBlock可能会更改
        if (condUnion.isNum && condUnion.getNumber() == 1) {
            //如果恒为1，则直接跳转到loopBlk
            builder.buildBrInst(condBlk, loopStartBlk);
        } else {
            builder.buildBrInst(condBlk, condUnion.getVariable(), loopStartBlk, finalBlk);
        }
        //从loop跳转到forStmt2Blk
        if (forStmt2Blk != null) {
            builder.buildBrInst(loopEndBlk, forStmt2Blk);
            builder.buildBrInst(forStmt2Blk, condBlk);
        } else {
            builder.buildBrInst(loopEndBlk, condBlk);
        }
        //最后把caller的basicBlock设为finalBlk
        assert caller instanceof BlockVisitor;
        ((BlockVisitor) caller).updateVisitingBlk(finalBlk);
    }

    @Override
    public void updateVisitingBlk(BasicBlock basicBlock) {
        this.basicBlock = basicBlock;
        if (caller != null && caller instanceof BlockController) {
            ((BlockController) caller).updateVisitingBlk(basicBlock);
        }
    }
}
