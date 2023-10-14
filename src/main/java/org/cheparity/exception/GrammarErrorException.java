package exception;

import lexer.dataStruct.Token;

public class GrammarErrorException extends RuntimeException {
    ErrorCode code;
    Token token;

    /**
     * Constructs a new runtime exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public GrammarErrorException(String message, ErrorCode code, Token token) {
        super(message);
        this.code = code;
        this.token = token;
    }

}
