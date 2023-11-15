package frontEnd.parser;

import frontEnd.lexer.dataStruct.Token;
import frontEnd.parser.dataStruct.ASTNode;

import java.util.List;

public interface SysYParser {
    ASTNode getAST();

    void setTokens(List<Token> tokens);

    void parse();
}
