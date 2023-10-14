package exception;

import lexer.dataStruct.Token;

public class RedundantRetStmtException extends GrammarErrorException {

    public RedundantRetStmtException(Token token) {
        super("Redundant return statement in (" + token.getLineNum() + "," + token.getColNum() + ")",
                ErrorCode.RETURN_STMT_REDUNDANT,
                token);
    }
}
