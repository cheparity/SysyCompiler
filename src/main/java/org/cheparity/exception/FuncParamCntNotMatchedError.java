package exception;

import frontEnd.lexer.dataStruct.Token;

public class FuncParamCntNotMatchedError extends GrammarError {
    private final int expect;
    private final int actual;

    public FuncParamCntNotMatchedError(int expect, int actual, Token token) {
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
