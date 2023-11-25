package middleEnd;

import frontEnd.parser.dataStruct.ASTNode;

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
    void visit(ASTNode node);
}
