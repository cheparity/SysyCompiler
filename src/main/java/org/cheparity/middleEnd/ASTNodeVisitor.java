package middleEnd;

import frontEnd.parser.dataStruct.ASTNode;
import middleEnd.llvm.ir.BasicBlock;
import middleEnd.llvm.ir.IrBuilder;
import utils.Message;

/**
 * 可能需要：
 * <p>
 * 1. GlobalVarDeclVisitor => Decl （全局变量）
 * <p>
 * 2. FuncDefVisitor => FuncDef & MainFuncDef
 * <p>
 * 3. EntryBlockVisitor => Block
 * <p>
 * 4. BlockItemVisitor => BlockItem
 */
public interface ASTNodeVisitor {
    BasicBlock basicblock = null;

    void visit(ASTNode node);

    IrBuilder getBuilder();

    void emit(Message message, ASTNodeVisitor sender);
}
