package exception;

import lexer.dataStruct.Token;

public class ValChangedException extends GrammarErrorException {

    public ValChangedException(Token token) {
        super("Const value cannot be changed in (" + token.getLineNum() + "," + token.getColNum() + ")",
                ErrorCode.VAL_CHANGED,
                token);
    }
}
