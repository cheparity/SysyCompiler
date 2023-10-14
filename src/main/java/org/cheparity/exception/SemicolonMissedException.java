package exception;

import lexer.dataStruct.Token;

public class SemicolonMissedException extends GrammarErrorException {

    public SemicolonMissedException(Token token) {
        super("Lack of semicolon in (" + token.getLineNum() + "," + token.getColNum() + ")",
                ErrorCode.SEMICOLON_LACKED,
                token);
    }
}
