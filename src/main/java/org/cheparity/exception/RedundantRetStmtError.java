package exception;

import frontEnd.lexer.dataStruct.Token;

public class RedundantRetStmtError extends GrammarError {

    public RedundantRetStmtError(Token token) {
        super("Redundant return statement in (" + token.getLineNum() + "," + token.getColNum() + ")",
                ErrorCode.RETURN_STMT_REDUNDANT,
                token);
    }
}
