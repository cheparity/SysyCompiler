package exception;

import frontEnd.lexer.dataStruct.Token;

public class PrintfUnmatchedError extends GrammarError {
    public PrintfUnmatchedError(Token token) {
        super("Unmatched params in printf at line " + token.getLineNum() + ", column " + token.getColNum() + ".",
                ErrorCode.PRINTF_UNMATCHED,
                token);
    }
}
