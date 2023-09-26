package exception;

public class TokenTypeErrorException extends RuntimeException {
    public TokenTypeErrorException(String s) {
        super(s);
    }
}
