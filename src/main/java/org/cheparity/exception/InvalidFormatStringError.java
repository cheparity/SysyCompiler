package exception;

import frontEnd.lexer.dataStruct.Token;

public class InvalidFormatStringError extends GrammarError {

    public InvalidFormatStringError(Token token) {
        super("Invalid format string in (" + token.getLineNum() + "," + token.getColNum() + ")",
                ErrorCode.ILLEGAL_SYMBOL, token);
    }
}
