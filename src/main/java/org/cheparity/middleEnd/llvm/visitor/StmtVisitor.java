package middleEnd.llvm.visitor;

import frontEnd.parser.dataStruct.ASTLeaf;
import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import middleEnd.ASTNodeVisitor;
import middleEnd.llvm.BlockController;
import middleEnd.llvm.ir.*;
import middleEnd.llvm.utils.NodeUnion;
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
            LOGGER.info("visit returnStmt: " + stmt.getRawValue());
            visitRetStmt(stmt);
        }
        //Stmt -> LVal '=' 'getint''('')'';'
        else if (stmt.deepDownFind(GrammarType.GETINT, 1).isPresent()) {
            LOGGER.info("visit getintStmt: " + stmt.getRawValue());
            visitGetintStmt(stmt);
        }
        //stmt -> 'printf''('FormatString{','Exp}')'';'
        else if (stmt.getChild(0).getGrammarType() == GrammarType.PRINTF) {
            LOGGER.info("visit printfStmt: " + stmt.getRawValue());

            visitPrintfStmt(stmt);
        }
        //Stmt -> LVal '=' Exp ';'
        else if (stmt.getChild(0).getGrammarType() == GrammarType.LVAL) {
            LOGGER.info("visit lvalStmt: " + stmt.getRawValue());
            visitLvalStmt(stmt);
        }
        //Stmt -> Exp ';'
        else if (stmt.getChild(0).getGrammarType() == GrammarType.EXP) {
            LOGGER.info("visit expStmt: " + stmt.getRawValue());
            new IrUtil(builder, basicBlock).calcAloExp(stmt.getChild(0));
        }
        //Stmt -> 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
        else if (stmt.getChild(0).getGrammarType() == GrammarType.IF) {
            LOGGER.info("visit ifStmt: " + stmt.getRawValue());
            visitIfStmt(stmt);
        }
        //Stmt -> 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
        else if (stmt.getChild(0).getGrammarType() == GrammarType.FOR) {
            LOGGER.info("visit forStmt: " + stmt.getRawValue());
            visitForStmt(stmt);
        }
        //Stmt -> Block
        else if (stmt.getChild(0).getGrammarType() == GrammarType.BLOCK) {
            LOGGER.info("visit blockStmt: " + stmt.getRawValue());
            stmt.accept(new BlockVisitor(basicBlock, this)); //这自动就处理匿名内部类的问题了
        }
        //Stmt -> 'break' ';'
        else if (stmt.getChild(0).getGrammarType() == GrammarType.BREAK) {
            LOGGER.info("visit breakStmt: " + stmt.getRawValue());
            visitBreakStmt();
        }
        //Stmt -> 'continue' ';'
        else if (stmt.getChild(0).getGrammarType() == GrammarType.CONTINUE) {
            LOGGER.info("visit continueStmt: " + stmt.getRawValue());
            visitContinueStmt();
        }
        if (!this.messages.isEmpty()) {
            //如果有未能处理的消息，发给caller继续处理
            LOGGER.info(this + " has " + this.messages.size() + " messages to send to caller.");
            this.messages.forEach(message -> caller.emit(message, this));
            this.messages.clear();
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
                LOGGER.info(this + " catches message " + message.request + " , and stop broadcasting.");
                this.messages.add(message);
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
                builder.buildBrInst(true, basicBlock, finalBlk);
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
                builder.buildBrInst(true, basicBlock, forStmt2);
                LOGGER.info("execute callback of forStmt2 " + forStmt2.getName());
            }
        };
        Message<CallBack<BasicBlock>> callBackMessage = new Message<>(callBack, "continueReq");
        LOGGER.info(this + " send the message of " + callBackMessage);
        caller.emit(callBackMessage, this);
    }

    private PointerValue visitLValAssign(ASTNode lval) {
        //visit LVal
        //LVal -> Ident {'[' Exp ']'}
        String ident = lval.getChild(0).getIdent();
        assert ident != null;

        var symbol = symbolTable.getSymbolSafely(ident, lval);
        PointerValue pointer = symbol.getPointer();
        assert pointer != null;
        if (symbol.getDim() == 0) {
            return pointer;
        } else if (pointer.getType().isArray()) {
            //比如a[1][2] = 3，a[2] = 5这种，得先getelementptr出来才能store
            NodeUnion offset = new IrUtil(builder, basicBlock).calcOffset(lval, symbol);
            pointer = builder.buildElementPointer(basicBlock, pointer, offset);
        } else if (pointer.getType().isPointer()) {
            //说明是指针的指针！
            //得先load出来得到数组指针，再getelementptr load出来的值，最后再store
            NodeUnion offset = new IrUtil(builder, basicBlock).calcOffset(lval, symbol);
            Variable pointerVariable = builder.buildLoadInst(basicBlock, pointer);
            PointerValue truePointer = builder.variableToPointer(pointerVariable);
            pointer = builder.buildElementPointer(basicBlock, truePointer, offset);
        }
        return pointer;
    }

    private void visitLvalStmt(ASTNode lvalStmt) {
        //Stmt -> LVal '=' Exp ';'
        ASTNode lval = lvalStmt.getChild(0);
        PointerValue pointer = visitLValAssign(lval);
        //visit Exp
        NodeUnion result = new IrUtil(builder, basicBlock).calcAloExp(lvalStmt.getChild(2));
        if (result.isNum) {
            builder.buildStoreInst(basicBlock, builder.buildConstIntNum(result.getNumber()), pointer);
            pointer.setNumber(result.getNumber());
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
        PointerValue pointer = visitLValAssign(lval);
        Variable variable = builder.buildCallInst(basicBlock, "getint");
        builder.buildStoreInst(basicBlock, variable, pointer);
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
        BasicBlock ifTrueBlk, finalBlk, elseBlk = null, entryBlock = basicBlock;
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
        cond = builder.toBitVariable(basicBlock, cond);

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
            builder.buildBrInst(false, entryBlock, cond, ifTrueBlk, elseBlk); //entryBlock 根据 条件 -> ifTrueBlk | elseBlk
            builder.buildBrInst(false, ifTrueBlk, finalBlk); //ifTrueBlk -> finalBlk todo why? 应该是ifTrueBlk的最后一句！
            builder.buildBrInst(false, elseBlk, finalBlk); //elseBlk -> finalBlk
        } else {
            builder.buildBrInst(false, entryBlock, cond, ifTrueBlk, finalBlk); //entryBlock 根据 条件 -> ifTrueBlk | finalBlk
            builder.buildBrInst(false, ifTrueBlk, finalBlk); //ifTrueBlk -> finalBlk
        }
        //这里应该是调用者的basicBlock = finalBlock？
        // 其实可以通过传递caller的方式传递过来
        assert caller instanceof BlockController;
        ((BlockController) caller).updateVisitingBlk(finalBlk);
    }

    //Stmt -> 'for' '(' [ForStmt1] ';' [Cond] ';' [ForStmt2] ')' Stmt
    private void visitForStmt(ASTNode forStmt) {
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

        //处理forStmt1 => 只有它，不用新建一个基本块
        if (forStmt1 != null) {
            // ForStmt -> LVal '=' Exp，是Stmt的一种特殊情况
            forStmt1.accept(new StmtVisitor(basicBlock, this));
        }

        BasicBlock condBlk, loopStartBlk, forStmt2Blk, finalBlk, loopEndBlk, beforeForBlk;
        beforeForBlk = basicBlock.setTag("beforeFor");

        //处理condition
        condBlk = builder.buildBasicBlock(basicBlock, symbolTable).setTag("cond");
        Variable condVariable; //不能优化！后面会变！！
        if (cond != null) {
            NodeUnion nodeUnion = new IrUtil(builder, condBlk).calcLogicExp(cond);
            //如果是数字，这下真说明是常量了
            if (nodeUnion.isNum && nodeUnion.getNumber() == 0) {
                basicBlock.removeBlock(condBlk);
                return;
            } else if (!nodeUnion.isNum) {
                condVariable = nodeUnion.getVariable();
            } else { //nodeUnion.isNum && nodeUnion.getNumber() == 0
                condVariable = builder.buildConstValue(1, IrType.IrTypeID.BitTyID);
            }
        } else {
            condVariable = builder.buildConstValue(1, IrType.IrTypeID.BitTyID);
        }
        condVariable = builder.toBitVariable(basicBlock, condVariable);

        //处理loop循环体
        //此时basicBlock的意义还是beforeForBlk，即for语句所在的块
        loopStartBlk = builder.buildBasicBlock(condBlk, symbolTable).setTag("forBody");
        loopStmt.accept(new StmtVisitor(loopStartBlk, this));

        //处理forStmt2
        forStmt2Blk = builder.buildBasicBlock(loopStartBlk, symbolTable).setTag("forStmt2");
        if (forStmt2 != null) {
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
        //新建一个基本块
        if (!this.messages.isEmpty()) {
            LOGGER.info("handle message list of " + this.messages.size() + " messages");
            messages.stream().filter(message -> message.request.equals("breakReq")).toList().forEach(message -> {
                ((CallBack<BasicBlock>) message.data).run(finalBlk);
                messages.remove(message);
            });

            messages.stream().filter(message -> message.request.equals("continueReq")).toList().forEach(message -> {
                ((CallBack<BasicBlock>) message.data).run(forStmt2Blk);
                messages.remove(message);
            });
        }

        builder.buildBrInst(false, beforeForBlk, condBlk); //cond处理完了，basicBlock直接跳，因为后面basicBlock可能会更改
        builder.buildBrInst(false, condBlk, condVariable, loopStartBlk, finalBlk);
        builder.buildBrInst(false, forStmt2Blk, condBlk);
        builder.buildBrInst(false, loopEndBlk, forStmt2Blk);
        //最后把caller的basicBlock设为finalBlk
        assert caller instanceof BlockVisitor;
        ((BlockVisitor) caller).updateVisitingBlk(finalBlk);

    }

    @Override
    public void updateVisitingBlk(BasicBlock basicBlock) {
        LOGGER.info("Update visiting block to " + basicBlock.getName());
        assert basicBlock.getSymbolTable() != null;
        this.basicBlock = basicBlock;
        if (caller != null && caller instanceof BlockController) {
            ((BlockController) caller).updateVisitingBlk(basicBlock);
        }
    }
}
