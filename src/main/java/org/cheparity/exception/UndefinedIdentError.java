package exception;

import lexer.dataStruct.Token;

public class UndefinedIdentError extends GrammarError {

    public UndefinedIdentError(Token token) {
        super("Undefined identifier in (" + token.getLineNum() + "," + token.getColNum() + ")",
                ErrorCode.IDENT_UNDEFINED,
                token);
    }
}
