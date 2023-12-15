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
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public final class StmtVisitor implements ASTNodeVisitor, BlockController {
    private static final Logger LOGGER = LoggerUtil.getLogger();
    final IrBuilder builder;
    final SymbolTable symbolTable;
    private final LinkedList<Message> messages = new LinkedList<>();
    private final ASTNodeVisitor caller;
    private ASTNode nodeVisiting;
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
        if (message.request.equals("stop visiting following stmts!")) {
            if (nodeVisiting.getChild(0).getGrammarType() == GrammarType.IF
                    || nodeVisiting.getChild(0).getGrammarType() == GrammarType.FOR) {
                return;//截断不发送
            }
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
        emitSkipMessage();
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
        emitSkipMessage();
    }

    private void emitSkipMessage() {
        LOGGER.info(this + " send the message of stop visiting following stmts!");
        caller.emit(new Message<>(null, "stop visiting following stmts!"), this);
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
            //重要！检查参数类型的一致性！
            if (res.isNum) {
                args[i] = builder.buildConstValue(res.getNumber(), IrType.IrTypeID.Int32TyID);
            } else if (res.getVariable().getType().isNumber()) {
                args[i] = res.getVariable();
            } else {
                args[i] = builder.buildLoadInst(basicBlock, builder.variableToPointer(res.getVariable()));
            }
        }
        //解析formatString
        char[] chars = formatString.toCharArray();
        int argCnt = 0;
        for (int i = 1; i < chars.length - 1; i++) {
            if (chars[i] == '%' && chars[i + 1] == 'd') {
                builder.buildCallCoreInst(basicBlock, "putint", args[argCnt]);
                argCnt++;
                i++;
            } else if (chars[i] == '\\' && chars[i + 1] == 'n') {
                builder.buildCallCoreInst(basicBlock, "putch", builder.buildConstValue(10, IrType.IrTypeID.Int32TyID));
                i++;
            } else {
                builder.buildCallCoreInst(basicBlock, "putch", builder.buildConstValue(chars[i], IrType.IrTypeID.Int32TyID));
            }
        }
    }

    private void visitGetintStmt(ASTNode getintStmt) {
        var lval = getintStmt.getChild(0);
        PointerValue pointer = visitLValAssign(lval);
        Variable variable = builder.buildCallCoreInst(basicBlock, "getint");
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

    //引入message机制
    private void visitCond(ASTNode condNode,
                           AtomicReference<BasicBlock> successBlockAtom,
                           AtomicReference<BasicBlock> failBlockAtom,
                           ASTNode ifTrueStmt
    ) {
//        if (condNode==null) {
//            //condition为null
//            var condBlk = builder.buildBasicBlock(basicBlock,basicBlock.getSymbolTable()).setTag("cond");
//            var successBlk = builder.buildBasicBlock(condBlk,basicBlock.getSymbolTable()).setTag("successBlk");
//            ifTrueStmt.accept(new StmtVisitor(successBlk,this));
//            var failBlk = builder.buildBasicBlock(basicBlock,symbolTable);
//
//        }

        LinkedList<ASTNode> expNodes = new LinkedList<>();
        IrUtil.unwrapAllLogicNodes(condNode, expNodes);
        BasicBlock nowBlk = null, successBlk, failBlk;
        BasicBlock[] blkList = new BasicBlock[expNodes.size()];
        NodeUnion[] condUnions = new NodeUnion[expNodes.size()]; //与blkList是伴生的关系

        //先建立block
        for (int i = 0; i < expNodes.size(); i++) {
            if (expNodes.get(i).getGrammarType() == GrammarType.EQ_EXP) {
                if (nowBlk == null) {
                    nowBlk = builder.buildBasicBlock(basicBlock, basicBlock.getSymbolTable()).setTag("condInline");
                    builder.buildBrInst(false, basicBlock, nowBlk);
                } else {
                    nowBlk = builder.buildBasicBlock(nowBlk, nowBlk.getSymbolTable()).setTag("condInline");
                }
                NodeUnion cond = new IrUtil(builder, nowBlk).calcLogicExp(expNodes.get(i));
                builder.toBitVariable(nowBlk, cond);
                condUnions[i] = cond;
                blkList[i] = nowBlk;
            } else {
                condUnions[i] = null;
                blkList[i] = null;
            }
        }
        assert nowBlk != null;
        successBlk = builder.buildBasicBlock(nowBlk, nowBlk.getSymbolTable()).setTag("successBlk");
        if (ifTrueStmt != null) {
            ifTrueStmt.accept(new StmtVisitor(successBlk, this));
        }
        failBlk = builder.buildBasicBlock(nowBlk, nowBlk.getSymbolTable()).setTag("failBlk");

        //再构建br
        for (int i = 0; i < expNodes.size(); i++) {
            if (blkList[i] != null) continue;
            GrammarType op = expNodes.get(i).getGrammarType();
            if (op == GrammarType.LOGICAL_AND) {
                //寻找下一个||后的块
                int j;
                var belonging = blkList[i - 1];
                var cond = condUnions[i - 1];
                var ifTureBlk = blkList[i + 1]; //继续解析&&后的块
                for (j = i; j < expNodes.size() && expNodes.get(j).getGrammarType() != GrammarType.LOGICAL_OR; j++) ;
                var ifFalseBlk = (j >= expNodes.size()) ? failBlk : blkList[j + 1];
                builder.buildBrInst(false, belonging, cond, ifTureBlk, ifFalseBlk); //finalBlk一般是ifTrue的Blk！！
            } else {
                var belonging = blkList[i - 1];
                var ifFalseBlk = blkList[i + 1]; //成功跳转success，失败则继续解析||右边的块
                var cond = condUnions[i - 1];
                builder.buildBrInst(false, belonging, cond, successBlk, ifFalseBlk);
            }
        }

        //remove finalBlk
//        condBlockAtom.set(blkList[blkList.length - 1].setTag("condLast")); //返回是最后一个块
        successBlockAtom.set(successBlk);
        failBlockAtom.set(failBlk);
//        return condUnions[expNodes.size() - 1]; //和最后一个块的条件语句
    }

    //Stmt -> 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
    private void visitIfStmt(ASTNode ifStmt) {
        BasicBlock ifTrueBlk, finalBlk, elseBlk, entryBlock = basicBlock;
        final boolean hasElseStmt = ifStmt.deepDownFind(GrammarType.ELSE, 1).isPresent();
        final ASTNode condNode = ifStmt.getChild(2);
        final ASTNode ifTrueNodeStmt = ifStmt.getChild(4);

        AtomicReference<BasicBlock> ifTrueBlkAtom = new AtomicReference<>();
        AtomicReference<BasicBlock> ifFalseBlkAtom = new AtomicReference<>();
        //如果cond可以直接判断，则没必要构建if的结构
        visitCond(condNode, ifTrueBlkAtom, ifFalseBlkAtom, ifTrueNodeStmt);
        //if True
        ifTrueBlk = ifTrueBlkAtom.get().setTag("ifTrue");
        //else
        if (hasElseStmt) {
            var elseStmt = ifStmt.getChild(-1);
            elseBlk = ifFalseBlkAtom.get().setTag("else");
            elseStmt.accept(new StmtVisitor(elseBlk, this));
            finalBlk = builder.buildBasicBlock(basicBlock, basicBlock.getSymbolTable()).setTag("ifEnd");//新建一个基本块
            builder.buildBrInst(false, ifTrueBlk, finalBlk);
            builder.buildBrInst(false, elseBlk, finalBlk);
        } else {
            finalBlk = ifFalseBlkAtom.get().setTag("ifEnd");
            builder.buildBrInst(false, ifTrueBlk, finalBlk);
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

        var entryBlk = basicBlock.setTag("beforeFor");

        //处理forStmt1 => 只有它，不用新建一个基本块
        if (forStmt1 != null) {
            // ForStmt -> LVal '=' Exp，是Stmt的一种特殊情况
            forStmt1.accept(new StmtVisitor(basicBlock, this));
        }

        BasicBlock forStmt2Blk, finalBlk; //必建块

        if (cond == null) {
            //1 构建loopStart循环块
            //2 从entryBlock跳转到loopStart
            //3 从loop跳转到forStmt2块，其中forStmt2块，有没有都得建立
            //4 forStmt2块跳转到loopStart（因为没有condition）
            BasicBlock loopStartBlk = builder.buildBasicBlock(entryBlk);
            loopStmt.accept(new StmtVisitor(loopStartBlk, this));

            BasicBlock loopEndBlk = basicBlock.setTag("loopEnd");
            forStmt2Blk = builder.buildBasicBlock(loopEndBlk);
            if (forStmt2 != null) {
                forStmt2.accept(new StmtVisitor(forStmt2Blk, this));
            }
            builder.buildBrInst(false, entryBlk, loopStartBlk);
            builder.buildBrInst(false, loopEndBlk, forStmt2Blk);
            builder.buildBrInst(false, forStmt2Blk, loopStartBlk);
        } else {
            //cond != null
            AtomicReference<BasicBlock> loopStartBlkAtom = new AtomicReference<>();
            AtomicReference<BasicBlock> forStmt2BlkAtom = new AtomicReference<>();
            visitCond(cond, loopStartBlkAtom, forStmt2BlkAtom, loopStmt);
            BasicBlock loopEndBlk = loopStartBlkAtom.get().setTag("loopEnd");
            forStmt2Blk = forStmt2BlkAtom.get();
            if (forStmt2 != null) {
                forStmt2.accept(new StmtVisitor(forStmt2Blk, this));
            }
//            builder.buildBrInst(false, entryBlk, loopStartBlk); //已经在cond里做过了
            builder.buildBrInst(false, loopEndBlk, forStmt2Blk);
            builder.buildBrInst(false, forStmt2Blk, loopStartBlk);
        }


        finalBlk = builder.buildBasicBlock(forStmt2Blk, entryBlk.getSymbolTable());

        //处理一下消息
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

    @Override
    public ASTNode getVisitingNode() {
        return this.nodeVisiting;
    }


    @Override
    public void visit(ASTNode stmt) {
        nodeVisiting = stmt;
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

}
