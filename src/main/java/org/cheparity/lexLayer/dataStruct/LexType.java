package lexLayer.dataStruct;

import exception.LexError;
import utils.LoggerUtil;
import utils.RegUtil;

import java.util.Arrays;
import java.util.regex.Pattern;

public enum LexType {
    // Operators
    NOT("!"),
    MULT("*"),
    ASSIGN("="),
    AND("&&"),
    OR("||"),
    MOD("%"),
    DIV("/"),
    PLUS("+"),
    MINU("-"),
    LSS("<"),
    LEQ("<="),
    GRE(">"),
    GEQ(">="),
    EQL("=="),
    NEQ("!="),

    // Punctuation
    SEMICN(";"),
    COMMA(","),
    LPARENT("("),
    RPARENT(")"),
    LBRACK("["),
    RBRACK("]"),
    LBRACE("{"),
    RBRACE("}"),

    // Keywords
    CONSTTK("const"),
    INTTK("int"),
    VOIDTK("void"),
    MAINTK("main"),
    IFTK("if"),
    ELSETK("else"),
    FORTK("for"),
    GETINTTK("getint"),
    PRINTFTK("printf"),
    RETURNTK("return"),
    CONTINUETK("continue"),
    BREAKTK("break"),

    // Identifiers and Literals
    IDENFR(""),
    INTCON(""),
    STRCON("");

    private final String value;

    LexType(String value) {
        this.value = value;
    }

    /**
     * Find LexType Enum corresponding with value.
     *
     * @param value The given value.
     * @return The corresponding LexType.
     */
    public static LexType ofValue(String value) {
        assert (value != null);
        var res = Arrays.stream(LexType.values())
                .filter(lexType -> lexType.getValue().equals(value)).findFirst();
        if (res.isPresent()) {
            return res.get();
        }
        if (Pattern.matches(RegUtil.NUM_REG, value)) {
            return INTCON;
        }
        if (Pattern.matches(RegUtil.STR_REG, value)) {
            return STRCON;
        }
        if (Pattern.matches(RegUtil.IDENT_REG, value)) {
            return IDENFR;
        }
        LoggerUtil.getLogger().severe("LexType [" + value + "] not found.");
        throw new LexError("LexType [" + value + "] not found.");
    }

    public String getValue() {
        return value;
    }

    /**
     * Check if the LexType is a keyword.
     *
     * @return True if the LexType is a keyword, false otherwise.
     */
    public boolean keyword() {
        return this.compareTo(CONSTTK) >= 0 && this.compareTo(BREAKTK) <= 0;
    }

    /**
     * Check if the LexType is an operator.
     *
     * @return True if the LexType is an operator, false otherwise.
     */
    public boolean operator() {
        return this.compareTo(NEQ) <= 0;
    }

    /**
     * Check if the LexType is punctuation.
     *
     * @return True if the LexType is punctuation, false otherwise.
     */
    public boolean punctuation() {
        return this.compareTo(SEMICN) >= 0 && this.compareTo(RBRACE) <= 0;
    }
}