package frontEnd.parser.dataStruct;

import frontEnd.lexer.dataStruct.LexType;

import java.util.Arrays;
import java.util.Optional;

public enum GrammarType {
    // Terminals
    IDENT("IDENTIFIER"),

    INT_CONST("IntConst"),

    // Keywords
    INT("int"),
    MAIN("main"),
    CONST("const"),
    VOID("void"),
    IF("if"),
    ELSE("else"),
    FOR("for"),
    BREAK("break"),
    CONTINUE("continue"),
    RETURN("return"),
    GETINT("getint"),
    PRINTF("printf"),

    // Operators
    PLUS("+"),
    MINUS("-"),
    MULTIPLY("*"),
    DIVIDE("/"),
    MOD("%"),
    ASSIGN("="),
    EQUAL("=="),
    NOT_EQUAL("!="),
    LESS_THAN("<"),
    GREATER_THAN(">"),
    LESS_THAN_EQUAL("<="),
    GREATER_THAN_EQUAL(">="),
    LOGICAL_AND("&&"),
    LOGICAL_OR("||"),
    NOT("!"),

    // Punctuation
    SEMICOLON(";"),
    COMMA(","),
    LEFT_PAREN("("),
    RIGHT_PAREN(")"),
    LEFT_BRACE("{"),
    RIGHT_BRACE("}"),
    LEFT_BRACKET("["),
    RIGHT_BRACKET("]"),

    // Non-terminals, or to say, grammar units
    COMP_UNIT("CompUnit"),
    DECL("Decl"),
    CONST_DECL("ConstDecl"),
    VAR_DEF("VarDef"),
    VAR_DECL("VarDecl"),
    FUNC_DEF("FuncDef"),
    B_TYPE("BType"),
    MAIN_FUNC_DEF("MainFuncDef"),
    CONST_DEF("ConstDef"),
    FUNC_TYPE("FuncType"),
    FUNC_FPARAMS("FuncFParams"),
    FUNC_FPARAM("FuncFParam"),
    BLOCK("Block"),
    BLOCK_ITEM("BlockItem"),
    STMT("Stmt"),
    FOR_STMT("ForStmt"),
    EXP("Exp"),
    REL_EXP("RelExp"),
    ADD_EXP("AddExp"),
    EQ_EXP("EqExp"),
    MUL_EXP("MulExp"),
    LAND_EXP("LAndExp"),
    LOR_EXP("LOrExp"),
    PRIMARY_EXP("PrimaryExp"),
    LVAL("LVal"),
    UNARY_EXP("UnaryExp"),
    UNARY_OP("UnaryOp"),
    CONST_EXP("ConstExp"),
    COND("Cond"),
    CONST_INIT_VAL("ConstInitVal"),
    INIT_VAL("InitVal"),
    FUNC_RPARAMS("FuncRParams"),
    FORMAT_STRING("FormatString"),
    NUMBER("Number"),
    ;


    private final String value;

    GrammarType(String value) {
        this.value = value;
    }

    public static Optional<GrammarType> ofTerminal(LexType lexType) {
        if (lexType.isPreserved()) {
            return Arrays.stream(GrammarType.values()).filter(grammar -> grammar.getValue().equals(lexType.getValue())).findFirst();
        }
        if (lexType.isIdent()) {
            return Optional.of(GrammarType.IDENT);
        }
        if (lexType.isIntConst()) {
            return Optional.of(GrammarType.INT_CONST);
        }
        if (lexType.isString()) {
            return Optional.of(GrammarType.FORMAT_STRING);
        }
//        LoggerUtil.getLogger().severe(lexType + " is not a terminal!");
        return Optional.empty();
    }

    public String getValue() {
        return value;
    }

    // Helper method to check if a token is a keyword
    public boolean isKeyword() {
        return this.compareTo(INT) >= 0 && this.compareTo(PRINTF) <= 0;
    }

    // Helper method to check if a token is an operator
    public boolean isOperator() {
        return this.compareTo(PLUS) >= 0 && this.compareTo(NOT) <= 0;
    }

    // Helper method to check if a token is punctuation
    public boolean isPunctuation() {
        return this.compareTo(SEMICOLON) >= 0 && this.compareTo(RIGHT_BRACKET) <= 0;
    }

    // Helper method to check if a token is a non-terminal
    public boolean isNonTerminal() {
        return this.compareTo(COMP_UNIT) >= 0 && this.compareTo(FORMAT_STRING) <= 0;
    }

    public boolean isTerminal() {
        return !isNonTerminal();
    }
}