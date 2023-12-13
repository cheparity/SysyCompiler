package middleEnd.llvm.visitor;

import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import middleEnd.ASTNodeVisitor;
import middleEnd.llvm.BlockController;
import middleEnd.llvm.ir.BasicBlock;
import middleEnd.llvm.ir.IrBuilder;
import utils.LoggerUtil;
import utils.Message;

import java.util.LinkedList;
import java.util.logging.Logger;

/**
 * blockVisitor<font color='red'>不负责建立基本块</font>，只负责把visit的blockItem加入到给定的基本块中。
 * <p>
 * 如果需要visit匿名块，或者需要新建基本块，请在调用blockVisitor之前自行指定新块。
 */
public final class BlockVisitor implements ASTNodeVisitor, BlockController {
    private final static Logger LOGGER = LoggerUtil.getLogger();
    private final LinkedList<Message> messages = new LinkedList<>();
    private final IrBuilder builder;
    private final ASTNodeVisitor caller;
    private ASTNode nodeVisiting;
    /**
     * basicBlock可能会在{@link StmtVisitor}中，由于if/for语句的存在，被改变。
     */
    private BasicBlock basicBlock;

    /**
     * @param basicBlock 需要visit的块
     * @param caller     IrBuilder
     */
    public BlockVisitor(BasicBlock basicBlock, ASTNodeVisitor caller) {
        this.basicBlock = basicBlock;
        this.builder = caller.getBuilder();
        this.caller = caller;
    }

    public IrBuilder getBuilder() {
        return builder;
    }

    @Override
    public void emit(Message message, ASTNodeVisitor sender) {
        if (sender == this) {
            LOGGER.info(this + " cannot handle the message, continue to pass.");
            caller.emit(message, sender);
            return;
        }

        if (catchMessage(message, "stop visiting following stmts!")) {
            LOGGER.info(this + " catches message " + message.request + " , and stop broadcasting.");
            return;
        }
        //不需要捕捉
        LOGGER.info(this + " don't need to handle this message, continue to pass.");
        caller.emit(message, sender);
    }

    private boolean catchMessage(Message message, String... requests) {
        boolean isCaught = false;
        for (String request : requests) {
            if (message.request.equals(request)) {
                //要找的block接信息者：是由ifStmt或者forStmt产生的block
                //Stmt -> 'if' '(' Cond ')' **Stmt** [ 'else' **Stmt** ] && **Stmt** -> Block
                //Stmt -> 'for' '(' [ForStmt1] ';' [Cond] ';' [ForStmt2] ')' **Stmt** && **Stmt** -> Block
                ASTNode stmt = nodeVisiting.getFather();
                if (stmt.getGrammarType() != GrammarType.STMT) continue;
                if (stmt.getFather() == null) continue;
                GrammarType grammarType = stmt.getFather().getChild(0).getGrammarType();
                if (grammarType != GrammarType.IF && grammarType != GrammarType.FOR) continue;
                LOGGER.info(this + " catches message " + message.request + " , and stop broadcasting.");
                this.messages.add(message);
                isCaught = true;
            }
        }
        return isCaught;
    }

    public BasicBlock getBasicBlock() {
        return basicBlock;
    }

    @Override
    public void updateVisitingBlk(BasicBlock basicBlock) {
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
    public void visit(ASTNode block) {
        assert block.getGrammarType().equals(GrammarType.BLOCK);
        nodeVisiting = block;
        basicBlock.setSymbolTable(block.getSymbolTable());
        //block -> {blockItem}
        for (var child : block.getChildren()) {
            if (child.getGrammarType() != GrammarType.BLOCK_ITEM) continue;
            if (!this.messages.isEmpty()) {
                LOGGER.info(this + " catches message " + this.messages.getFirst().request + " , and stop visiting following stmts!");
                return;
            }
            child.accept(new StmtVisitor(this));
            child.accept(new LocalVarVisitor(this));
        }
    }

}
