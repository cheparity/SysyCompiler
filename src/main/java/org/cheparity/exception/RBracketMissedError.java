package exception;

import lexer.dataStruct.Token;

public class RBracketMissedError extends GrammarError {
    public RBracketMissedError(Token token) {
        super("Lack of right bracket at line " + token.getLineNum() + ", column " + token.getColNum() + ".",
                ErrorCode.RIGHT_BRACKET_LACKED,
                token);
    }
}
