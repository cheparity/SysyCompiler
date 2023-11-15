package frontEnd.lexer;

import frontEnd.lexer.dataStruct.Token;

import java.util.ArrayList;
import java.util.Optional;

public interface SysYLexer {

    Optional<Token> next();

    ArrayList<Token> getAllTokens();
}
