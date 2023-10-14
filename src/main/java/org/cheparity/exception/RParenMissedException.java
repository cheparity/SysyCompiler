package exception;

import lexer.dataStruct.Token;

public class RParenMissedException extends GrammarErrorException {
    public RParenMissedException(Token token) {
        super("Lack of right parenthesis at line " + token.getLineNum() + ", column " + token.getColNum() + ".",
                ErrorCode.RIGHT_PAREN_LACKED,
                token);
    }
}
