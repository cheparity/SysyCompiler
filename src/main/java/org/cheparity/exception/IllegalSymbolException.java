package exception;

import lexer.dataStruct.Token;

public class IllegalSymbolException extends GrammarErrorException {

    public IllegalSymbolException(Token token) {
        super("Illegal symbol in (" + token.getLineNum() + "," + token.getColNum() + ")", ErrorCode.ILLEGAL_SYMBOL, token);
    }
}
