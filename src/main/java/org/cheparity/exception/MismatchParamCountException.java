package exception;

import lexer.dataStruct.Token;

public class MismatchParamCountException extends GrammarErrorException {
    private final int expect;
    private final int actual;

    public MismatchParamCountException(int expect, int actual, Token token) {
        super("Mismatch parameter count, expect " + expect + ", actual " + actual,
                ErrorCode.PARAM_NUM_UNMATCHED,
                token);
        this.expect = expect;
        this.actual = actual;
    }

    public int getExpect() {
        return expect;
    }

    public int getActual() {
        return actual;
    }
}
