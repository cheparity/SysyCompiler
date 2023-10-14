package exception;

import lexer.dataStruct.Token;

public class DupIdentException extends GrammarErrorException {
    private final Token preToken;

    public DupIdentException(Token token, Token prevToken) {
        super("Duplicate identifier in (" + token.getLineNum() + "," + token.getColNum() + "), " +
                        "previous definition in (" + prevToken.getLineNum() + "," + prevToken.getColNum() + ")",
                ErrorCode.IDENT_DUP_DEFINED, token);
        this.preToken = prevToken;
    }

    public Token getPreToken() {
        return preToken;
    }
}
