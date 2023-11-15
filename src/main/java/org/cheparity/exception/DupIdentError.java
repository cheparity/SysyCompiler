package exception;

import frontEnd.lexer.dataStruct.Token;

public class DupIdentError extends GrammarError {
    private final Token preToken;

    public DupIdentError(Token token, Token prevToken) {
        super("Duplicate identifier in (" + token.getLineNum() + "," + token.getColNum() + "), " +
                        "previous definition in (" + prevToken.getLineNum() + "," + prevToken.getColNum() + ")",
                ErrorCode.IDENT_DUP_DEFINED, token);
        this.preToken = prevToken;
    }

    public Token getPreToken() {
        return preToken;
    }
}
