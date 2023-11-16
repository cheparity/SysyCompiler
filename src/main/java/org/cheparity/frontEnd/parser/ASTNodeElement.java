package frontEnd.parser;

import frontEnd.parser.dataStruct.GrammarType;
import middleEnd.ASTNodeVisitor;

public interface ASTNodeElement {
    void accept(ASTNodeVisitor visitor);

    GrammarType getGrammarType();
}
