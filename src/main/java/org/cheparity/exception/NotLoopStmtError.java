package exception;

import lexer.dataStruct.Token;

public class NotLoopStmtError extends GrammarError {
    public NotLoopStmtError(Token token) {
        super("Break or continue not in loop at line " + token.getLineNum() + ", column " + token.getColNum() + ".",
                ErrorCode.BREAK_CONTINUE_NOT_IN_LOOP,
                token);
    }
}
