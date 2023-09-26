package grammarLayer;

import grammarLayer.dataStruct.ASTNode;

import java.util.Optional;

public interface GrammarParser {
    /**
     * CompUnit → {Decl} {FuncDef} MainFuncDef
     *
     * @return Optional<ASTNode> representing the parsed CompUnit
     */
    Optional<ASTNode> parseCompUnit(ASTNode father);

    /**
     * Decl → ConstDecl | VarDecl
     *
     * @return Optional<ASTNode> representing the parsed Decl
     */
    Optional<ASTNode> parseDecl(ASTNode father);

    /**
     * ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
     *
     * @return Optional<ASTNode> representing the parsed ConstDecl
     */
    Optional<ASTNode> parseConstDecl(ASTNode father);

    /**
     * VarDecl → BType VarDef { ',' VarDef } ';'
     *
     * @return Optional<ASTNode> representing the parsed VarDecl
     */
    Optional<ASTNode> parseVarDecl(ASTNode father);

    /**
     * ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
     *
     * @return Optional<ASTNode> representing the parsed ConstDef
     */
    Optional<ASTNode> parseConstDef(ASTNode father);

    /**
     * ConstInitVal → ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
     *
     * @return Optional<ASTNode> representing the parsed ConstInitVal
     */
    Optional<ASTNode> parseConstInitVal(ASTNode father);

    /**
     * VarDef → Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal
     *
     * @return Optional<ASTNode> representing the parsed VarDef
     */
    Optional<ASTNode> parseVarDef(ASTNode father);

    /**
     * InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'
     *
     * @return Optional<ASTNode> representing the parsed InitVal
     */
    Optional<ASTNode> parseInitVal(ASTNode father);

    /**
     * FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
     *
     * @return Optional<ASTNode> representing the parsed FuncDef
     */
    Optional<ASTNode> parseFuncDef(ASTNode father);

    /**
     * MainFuncDef → 'int' 'main' '(' ')' Block
     *
     * @return Optional<ASTNode> representing the parsed MainFuncDef
     */
    Optional<ASTNode> parseMainFuncDef(ASTNode father);

    /**
     * FuncType → 'void' | 'int'
     *
     * @return Optional<ASTNode> representing the parsed FuncType
     */
    Optional<ASTNode> parseFuncType(ASTNode father);

    /**
     * FuncFParams → FuncFParam { ',' FuncFParam }
     *
     * @return Optional<ASTNode> representing the parsed FuncFParams
     */
    Optional<ASTNode> parseFuncFParams(ASTNode father);

    /**
     * FuncFParam → BType Ident ['[' ']' { '[' ConstExp ']' }]
     *
     * @return Optional<ASTNode> representing the parsed FuncFParam
     */
    Optional<ASTNode> parseFuncFParam(ASTNode father);

    /**
     * Block → '{' { BlockItem } '}'
     *
     * @return Optional<ASTNode> representing the parsed Block
     */
    Optional<ASTNode> parseBlock(ASTNode father);

    /**
     * BlockItem → Decl | Stmt
     *
     * @return Optional<ASTNode> representing the parsed BlockItem
     */
    Optional<ASTNode> parseBlockItem(ASTNode father);

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
    Optional<ASTNode> parseStmt(ASTNode father);

    /**
     * ForStmt → LVal '=' Exp
     *
     * @return Optional<ASTNode> representing the parsed ForStmt
     */
    Optional<ASTNode> parseForStmt(ASTNode father);

    /**
     * Exp → AddExp
     *
     * @return Optional<ASTNode> representing the parsed Exp
     */
    Optional<ASTNode> parseExp(ASTNode father);

    /**
     * Cond → LOrExp
     *
     * @return Optional<ASTNode> representing the parsed Cond
     */
    Optional<ASTNode> parseCond(ASTNode father);

    /**
     * LVal → Ident {'[' Exp ']'}
     *
     * @return Optional<ASTNode> representing the parsedLVal
     */
    Optional<ASTNode> parseLVal(ASTNode father);

    /**
     * AddExp → MulExp { ('+' | '-') MulExp }
     *
     * @return Optional<ASTNode> representing the parsed AddExp
     */
    Optional<ASTNode> parseAddExp(ASTNode father);

    /**
     * MulExp → UnaryExp { ('*' | '/' | '%') UnaryExp }
     *
     * @return Optional<ASTNode> representing the parsed MulExp
     */
    Optional<ASTNode> parseMulExp(ASTNode father);

    /**
     * UnaryExp → PostfixExp | ('+' | '-' | '!') UnaryExp
     *
     * @return Optional<ASTNode> representing the parsed UnaryExp
     */
    Optional<ASTNode> parseUnaryExp(ASTNode father);

    /**
     * PostfixExp → PrimaryExp { '[' Exp ']' } { '.' Ident } { '(' [Args] ')' }
     *
     * @return Optional<ASTNode> representing the parsed PostfixExp
     */
    Optional<ASTNode> parsePostfixExp(ASTNode father);

    /**
     * PrimaryExp → '(' Exp ')' | LVal | Number | String | 'sizeof' '(' TypeSpec ')'
     * | 'getint''('')' | 'getch''('')' | Ident '(' [Args] ')'
     *
     * @return Optional<ASTNode> representing the parsed PrimaryExp
     */
    Optional<ASTNode> parsePrimaryExp(ASTNode father);

    /**
     * Args → Exp { ',' Exp }
     *
     * @return Optional<ASTNode> representing the parsed Args
     */
    Optional<ASTNode> parseArgs(ASTNode father);

    /**
     * TypeSpec → BType | 'void'
     *
     * @return Optional<ASTNode> representing the parsed TypeSpec
     */
    Optional<ASTNode> parseTypeSpec(ASTNode father);

    /**
     * BType → 'int' | 'char'
     *
     * @return Optional<ASTNode> representing the parsed BType
     */
    Optional<ASTNode> parseBType(ASTNode father);

    /**
     * Number → Digit { Digit }
     *
     * @return Optional<ASTNode> representing the parsed Number
     */
    Optional<ASTNode> parseNumber(ASTNode father);

    /**
     * String → '"' { Char } '"'
     *
     * @return Optional<ASTNode> representing the parsed String
     */
    Optional<ASTNode> parseString(ASTNode father);

    /**
     * Char → [^"\n]
     *
     * @return Optional<ASTNode> representing the parsed Char
     */
    Optional<ASTNode> parseChar(ASTNode father);

    /**
     * Digit → [0-9]
     *
     * @return Optional<ASTNode> representing the parsed Digit
     */
    Optional<ASTNode> parseDigit(ASTNode father);
}