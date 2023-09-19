package lexLayer;

import exception.LexError;
import utils.LoggerUtil;
import utils.RegUtil;

import java.util.Arrays;
import java.util.regex.Pattern;

public enum LexType {
    NOT("!"),
    MULT("*"),
    ASSIGN("="),
    AND("&&"),
    DIV("/"),
    SEMICN(";"),
    OR("||"),
    MOD("%"),
    COMMA(","),
    MAINTK("main"),
    FORTK("for"),
    LSS("<"),
    LPARENT("("),
    CONSTTK("const"),
    GETINTTK("getint"),
    LEQ("<="),
    RPARENT(")"),
    INTTK("int"),
    PRINTFTK("printf"),
    GRE(">"),
    LBRACK("["),
    BREAKTK("break"),
    RETURNTK("return"),
    GEQ(">="),
    RBRACK("]"),
    CONTINUETK("continue"),
    PLUS("+"),
    EQL("=="),
    NEQ("!="),
    LBRACE("{"),
    IFTK("if"),
    MINU("-"),
    RBRACE("}"),
    ELSETK("else"),
    VOIDTK("void"),
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

}