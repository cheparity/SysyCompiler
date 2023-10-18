package exception;

import lexer.dataStruct.Token;

public class SemicolonMissedError extends GrammarError {

    public SemicolonMissedError(Token token) {
        super("Lack of semicolon in (" + token.getLineNum() + "," + token.getColNum() + ")",
                ErrorCode.SEMICOLON_LACKED,
                token);
    }
}
