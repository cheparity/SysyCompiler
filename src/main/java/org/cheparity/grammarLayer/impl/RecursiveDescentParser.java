package grammarLayer.impl;

import grammarLayer.dataStruct.ASTLeaf;
import grammarLayer.dataStruct.ASTNode;
import grammarLayer.dataStruct.GrammarType;
import lexLayer.LexicalParser;
import lexLayer.dataStruct.LexType;
import lexLayer.dataStruct.Token;
import lexLayer.impl.LexicalParserImpl;
import utils.LoggerUtil;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.logging.Logger;

public class RecursiveDescentParser {
    private final static Logger LOGGER = LoggerUtil.getLogger();
    private final LexicalParser lexicalParser = LexicalParserImpl.getInstance();
    private final ArrayList<Token> tokens = lexicalParser.getAllTokens();
    private ASTNode AST;
    private int nowIndex = 0;
    private Token now;

    private void error() {
        LOGGER.severe("Error");
    }

    private void next() {
        if (nowIndex < tokens.size() - 1) {
            nowIndex++;
            now = tokens.get(nowIndex);
        } else {
            LOGGER.info("End of tokens");
        }
    }

    /**
     * CompUnit → {Decl} {FuncDef} MainFuncDef
     *
     * @return Optional<ASTNode> representing the parsed CompUnit
     */
    private Optional<ASTNode> parseCompUnit(ASTNode father) {
        ASTNode compUnit = new ASTNode(GrammarType.COMP_UNIT, father);
        parseDecl(compUnit).ifPresentOrElse(compUnit::addChild, this::error);
        next();
        parseDecl(compUnit).ifPresentOrElse(compUnit::addChild, this::error);
        return Optional.of(compUnit);
    }

    /**
     * Decl → ConstDecl | VarDecl
     *
     * @return Optional<ASTNode> representing the parsed Decl
     */
    private Optional<ASTNode> parseDecl(ASTNode father) {
        ASTNode decl = new ASTNode(GrammarType.DECL, father);
        if (parseConstDecl(decl).isPresent()) {
            return parseConstDecl(decl);
        } else if (parseVarDecl(decl).isPresent()) {
            return parseVarDecl(decl);
        } else {
            error();
        }

        return Optional.empty();
    }

    /**
     * ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
     *
     * @return Optional<ASTNode> representing the parsed ConstDecl
     */
    private Optional<ASTNode> parseConstDecl(ASTNode father) {
        ASTNode constDecl = new ASTNode(GrammarType.CONST_DECL, father);

        parseTerminal(constDecl).ifPresentOrElse(constDecl::addChild, this::error);
        next();
        parseBType(constDecl).ifPresentOrElse(constDecl::addChild, this::error);
        next();
        parseConstDef(constDecl).ifPresentOrElse(constDecl::addChild, this::error);
        Optional<ASTLeaf> terminal;

        while ((terminal = parseTerminal(constDecl)).isPresent()) {

        }

        return Optional.empty();
    }


    /**
     * BType → 'int'
     *
     * @return Optional<ASTNode> representing the parsed BType
     */
    private Optional<ASTNode> parseBType(ASTNode father) {
        ASTNode bType = new ASTNode(now, GrammarType.INT, father);
        if (now.getLexType().equals(LexType.INTTK)) {
            father.addChild(bType);
            next();
            return Optional.of(bType);
        } else {
            error();
        }

        return Optional.empty();
    }

    /**
     * ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
     *
     * @return Optional<ASTNode> representing the parsed ConstDef
     */
    private Optional<ASTNode> parseConstDef(ASTNode father) {

        return Optional.empty();
    }


    /**
     * ConstInitVal → ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
     *
     * @return Optional<ASTNode> representing the parsed ConstInitVal
     */
    private Optional<ASTNode> parseConstInitVal(ASTNode father) {

        return Optional.empty();
    }

    /**
     * VarDecl → BType VarDef { ',' VarDef } ';'
     *
     * @return Optional<ASTNode> representing the parsed VarDecl
     */
    private Optional<ASTNode> parseVarDecl(ASTNode father) {

        return Optional.empty();
    }


    /**
     * VarDef → Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal
     *
     * @return Optional<ASTNode> representing the parsed VarDef
     */
    private Optional<ASTNode> parseVarDef(ASTNode father) {


        return Optional.empty();
    }

    /**
     * InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'
     *
     * @return Optional<ASTNode> representing the parsed InitVal
     */
    private Optional<ASTNode> parseInitVal(ASTNode father) {

        return Optional.empty();
    }

    /**
     * FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
     *
     * @return Optional<ASTNode> representing the parsed FuncDef
     */
    private Optional<ASTNode> parseFuncDef(ASTNode father) {

        return Optional.empty();
    }

    /**
     * MainFuncDef → 'int' 'main' '(' ')' Block
     *
     * @return Optional<ASTNode> representing the parsed MainFuncDef
     */
    private Optional<ASTNode> parseMainFuncDef(ASTNode father) {

        return Optional.empty();
    }

    /**
     * FuncType → 'void' | 'int'
     *
     * @return Optional<ASTNode> representing the parsed FuncType
     */
    private Optional<ASTNode> parseFuncType(ASTNode father) {
        return Optional.empty();
    }

    /**
     * FuncFParams → FuncFParam { ',' FuncFParam }
     *
     * @return Optional<ASTNode> representing the parsed FuncFParams
     */
    private Optional<ASTNode> parseFuncFParams(ASTNode father) {
        return Optional.empty();
    }

    /**
     * FuncFParam → BType Ident ['[' ']' { '[' ConstExp ']' }]
     *
     * @return Optional<ASTNode> representing the parsed FuncFParam
     */
    private Optional<ASTNode> parseFuncFParam(ASTNode father) {
        return Optional.empty();
    }

    /**
     * Block → '{' { BlockItem } '}'
     *
     * @return Optional<ASTNode> representing the parsed Block
     */

    private Optional<ASTNode> parseBlock(ASTNode father) {
        return Optional.empty();
    }

    /**
     * BlockItem → Decl | Stmt
     *
     * @return Optional<ASTNode> representing the parsed BlockItem
     */

    private Optional<ASTNode> parseBlockItem(ASTNode father) {
        return Optional.empty();
    }

    /**
     * Stmt → LVal '=' Exp ';'
     * | [Exp] ';'
     * | Block
     * | 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
     * | 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
     * | 'break' ';'
     * | 'continue' ';'
     * | 'return' [Exp] ';'
     * | LVal '=' 'getint''('')'';'
     * | 'printf''('FormatString{','Exp}')'';'
     *
     * @return Optional<ASTNode> representing the parsed Stmt
     */

    private Optional<ASTNode> parseStmt(ASTNode father) {
        return Optional.empty();
    }

    /**
     * ForStmt → LVal '=' Exp
     *
     * @return Optional<ASTNode> representing the parsed ForStmt
     */

    private Optional<ASTNode> parseForStmt(ASTNode father) {
        return Optional.empty();
    }

    /**
     * Exp → AddExp
     *
     * @return Optional<ASTNode> representing the parsed Exp
     */

    private Optional<ASTNode> parseExp(ASTNode father) {
        return Optional.empty();
    }

    /**
     * Cond → LOrExp
     *
     * @return Optional<ASTNode> representing the parsed Cond
     */

    private Optional<ASTNode> parseCond(ASTNode father) {
        return Optional.empty();
    }

    /**
     * LVal → Ident {'[' Exp ']'}
     *
     * @return Optional<ASTNode> representing the parsedLVal
     */

    private Optional<ASTNode> parseLVal(ASTNode father) {
        return Optional.empty();
    }

    /**
     * AddExp → MulExp { ('+' | '-') MulExp }
     *
     * @return Optional<ASTNode> representing the parsed AddExp
     */

    private Optional<ASTNode> parseAddExp(ASTNode father) {
        return Optional.empty();
    }

    /**
     * MulExp → UnaryExp { ('*' | '/' | '%') UnaryExp }
     *
     * @return Optional<ASTNode> representing the parsed MulExp
     */

    private Optional<ASTNode> parseMulExp(ASTNode father) {
        return Optional.empty();
    }

    /**
     * UnaryExp → PostfixExp | ('+' | '-' | '!') UnaryExp
     *
     * @return Optional<ASTNode> representing the parsed UnaryExp
     */

    private Optional<ASTNode> parseUnaryExp(ASTNode father) {
        return Optional.empty();
    }

    /**
     * PostfixExp → PrimaryExp { '[' Exp ']' } { '.' Ident } { '(' [Args] ')' }
     *
     * @return Optional<ASTNode> representing the parsed PostfixExp
     */

    private Optional<ASTNode> parsePostfixExp(ASTNode father) {
        return Optional.empty();
    }

    /**
     * PrimaryExp → '(' Exp ')' | LVal | Number | String | 'sizeof' '(' TypeSpec ')'
     * | 'getint''('')' | 'getch''('')' | Ident '(' [Args] ')'
     *
     * @return Optional<ASTNode> representing the parsed PrimaryExp
     */

    private Optional<ASTNode> parsePrimaryExp(ASTNode father) {
        return Optional.empty();
    }

    /**
     * Args → Exp { ',' Exp }
     *
     * @return Optional<ASTNode> representing the parsed Args
     */

    private Optional<ASTNode> parseArgs(ASTNode father) {
        return Optional.empty();
    }

    /**
     * TypeSpec → BType | 'void'
     *
     * @return Optional<ASTNode> representing the parsed TypeSpec
     */

    private Optional<ASTNode> parseTypeSpec(ASTNode father) {
        return Optional.empty();
    }


    /**
     * Number → IntConst
     *
     * @return Optional<ASTNode> representing the parsed Number
     */

    private Optional<ASTNode> parseNumber(ASTNode father) {
        ASTNode numberNode = new ASTNode(now, GrammarType.NUMBER, father);
        if (now.getLexType().number()) {
            father.addChild(numberNode);
            return Optional.of(numberNode);
        }
        return Optional.empty();
    }

    /**
     * String → '"' { Char } '"'
     *
     * @return Optional<ASTNode> representing the parsed String
     */

    private Optional<ASTNode> parseString(ASTNode father) {
        //todo wondering if this is necessary.
        ASTNode stringNode = new ASTNode(now, GrammarType.FORMAT_STRING, father);
        if (now.getLexType().equals(LexType.STRCON)) {
            father.addChild(stringNode);
            return Optional.of(stringNode);
        }
        return Optional.empty();
    }

    /**
     * Char → [^"\n]
     *
     * @return Optional<ASTNode> representing the parsed Char
     */

    private Optional<ASTNode> parseChar(ASTNode father) {
        //todo check if necessary.
        throw new RuntimeException("Not implement");
    }

    /**
     * Digit → [0-9]
     *
     * @return Optional<ASTNode> representing the parsed Digit
     */

    private Optional<ASTNode> parseDigit(ASTNode father) {
        Token token = tokens.get(nowIndex);
        ASTNode digitNode = new ASTNode(token, GrammarType.NUMBER, father);
        if (token.getLexType().number()) {
            father.addChild(digitNode);
            return Optional.of(digitNode);
        }
        return Optional.empty();
    }


    private Optional<ASTLeaf> parseTerminal(ASTNode father) {
        ASTLeaf terminal;
        try {
            terminal = new ASTLeaf(father, now);
        } catch (NoSuchElementException e) {
            return Optional.empty();
        }
        return Optional.of(terminal);
    }
}
