package exception;

import lexer.dataStruct.Token;

public class FuncParamTypeNotMatchedError extends GrammarError {
    private final String expect;
    private final String actual;

    public FuncParamTypeNotMatchedError(String expect, String actual, Token token) {
        super("Mismatch parameter type, expect " + expect + ", actual " + actual,
                ErrorCode.PARAM_TYPE_UNMATCHED,
                token);
        this.expect = expect;
        this.actual = actual;
    }

    public String getExpect() {
        return expect;
    }

    public String getActual() {
        return actual;
    }
}
