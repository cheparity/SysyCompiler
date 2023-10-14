package exception;

import lexer.dataStruct.Token;

public class NotLoopStmtException extends GrammarErrorException {
    public NotLoopStmtException(Token token) {
        super("Break or continue not in loop at line " + token.getLineNum() + ", column " + token.getColNum() + ".",
                ErrorCode.BREAK_CONTINUE_NOT_IN_LOOP,
                token);
    }
}
