package exception;

import lexer.dataStruct.Token;

public class GrammarError implements Comparable<GrammarError> {
    ErrorCode code;
    Token token;
    String message;

    public GrammarError(String message, ErrorCode code, Token token) {
        this.message = message;
        this.code = code;
        this.token = token;
    }

    public String getMessage() {
        return this.message;
    }

    public Token getToken() {
        return this.token;
    }

    public ErrorCode getCode() {
        return this.code;
    }

    @Override
    public int compareTo(GrammarError o) {
        if (this.token.getLineNum() == o.token.getLineNum()) {
            return this.token.getColNum() - o.token.getColNum();
        } else {
            return this.token.getLineNum() - o.token.getLineNum();
        }
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
