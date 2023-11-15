package middleEnd.visitor;

import frontEnd.parser.dataStruct.ASTNode;

public interface ASTNodeVisitor {
    void visit(ASTNode node);
}
