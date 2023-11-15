package exception;

import frontEnd.lexer.dataStruct.Token;

public class RetStmtMissedError extends GrammarError {

    public RetStmtMissedError(Token token) {
        super("Lack of return statement in (" + token.getLineNum() + "," + token.getColNum() + ")",
                ErrorCode.RETURN_STMT_LACKED,
                token);
    }
}
