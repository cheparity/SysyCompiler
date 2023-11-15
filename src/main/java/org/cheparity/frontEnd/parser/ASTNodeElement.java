package frontEnd.parser;

import frontEnd.parser.dataStruct.GrammarType;
import middleEnd.visitor.ASTNodeVisitor;

public interface ASTNodeElement {
    void accept(ASTNodeVisitor visitor);

    GrammarType getGrammarType();
}
