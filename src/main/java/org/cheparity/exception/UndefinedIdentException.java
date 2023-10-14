package exception;

import lexer.dataStruct.Token;

public class UndefinedIdentException extends GrammarErrorException {

    public UndefinedIdentException(Token token) {
        super("Undefined identifier in (" + token.getLineNum() + "," + token.getColNum() + ")",
                ErrorCode.IDENT_UNDEFINED,
                token);
    }
}
