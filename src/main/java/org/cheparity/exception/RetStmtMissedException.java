package exception;

import lexer.dataStruct.Token;

public class RetStmtMissedException extends GrammarErrorException {

    public RetStmtMissedException(Token token) {
        super("Lack of return statement in (" + token.getLineNum() + "," + token.getColNum() + ")",
                ErrorCode.RETURN_STMT_LACKED,
                token);
    }
}
