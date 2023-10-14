package exception;

import lexer.dataStruct.Token;

public class PrintfUnmatchedException extends GrammarErrorException {
    public PrintfUnmatchedException(Token token) {
        super("Unmatched params in printf at line " + token.getLineNum() + ", column " + token.getColNum() + ".",
                ErrorCode.PRINTF_UNMATCHED,
                token);
    }
}
