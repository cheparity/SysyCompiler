package exception;

import lexer.dataStruct.Token;

public class ConstChangedError extends GrammarError {

    public ConstChangedError(Token token) {
        super("Const value cannot be changed in (" + token.getLineNum() + "," + token.getColNum() + ")",
                ErrorCode.VAL_CHANGED,
                token);
    }
}
