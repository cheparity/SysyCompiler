package exception;

public enum ErrorCode {
    ILLEGAL_SYMBOL('a'),
    IDENT_DUP_DEFINED('b'),
    IDENT_UNDEFINED('c'),
    PARAM_NUM_UNMATCHED('d'),
    PARAM_TYPE_UNMATCHED('e'),
    RETURN_STMT_REDUNDANT('f'),
    RETURN_STMT_LACKED('g'),
    VAL_CHANGED('h'),
    SEMICOLON_LACKED('i'),
    RIGHT_PAREN_LACKED('j'),
    RIGHT_BRACKET_LACKED('k'),
    PRINTF_UNMATCHED('l'),
    BREAK_CONTINUE_NOT_IN_LOOP('m');
    private final char value;

    ErrorCode(char value) {
        this.value = value;
    }

    public char getValue() {
        return this.value;
    }

}
