package grammarLayer;

import grammarLayer.dataStruct.ASTLeaf;
import grammarLayer.dataStruct.ASTNode;
import grammarLayer.dataStruct.GrammarType;

import java.util.Optional;

public interface GrammarParser {
    /**
     * CompUnit → {Decl} {FuncDef} MainFuncDef
     *
     * @return Optional<ASTNode> representing the parsed CompUnit
     */
    Optional<ASTNode> parseCompUnit();

    /**
     * Decl → ConstDecl | VarDecl
     *
     * @return Optional<ASTNode> representing the parsed Decl
     */
    Optional<ASTNode> parseDecl();

    /**
     * ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
     *
     * @return Optional<ASTNode> representing the parsed ConstDecl
     */
    Optional<ASTNode> parseConstDecl();

    /**
     * BType → 'int'
     *
     * @return Optional<ASTNode> representing the parsed BType
     */
    Optional<ASTNode> parseBType();

    /**
     * ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
     *
     * @return Optional<ASTNode> representing the parsed ConstDef
     */
    Optional<ASTNode> parseConstDef();

    /**
     * ConstInitVal → ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
     *
     * @return Optional<ASTNode> representing the parsed ConstInitVal
     */
    Optional<ASTNode> parseConstInitVal();

    /**
     * VarDecl → BType VarDef { ',' VarDef } ';'
     *
     * @return Optional<ASTNode> representing the parsed VarDecl
     */
    Optional<ASTNode> parseVarDecl();


    /**
     * VarDef → Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal
     *
     * @return Optional<ASTNode> representing the parsed VarDef
     */
    Optional<ASTNode> parseVarDef();

    /**
     * InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'
     *
     * @return Optional<ASTNode> representing the parsed InitVal
     */
    Optional<ASTNode> parseInitVal();

    /**
     * FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
     *
     * @return Optional<ASTNode> representing the parsed FuncDef
     */
    Optional<ASTNode> parseFuncDef();

    /**
     * MainFuncDef → 'int' 'main' '(' ')' Block
     *
     * @return Optional<ASTNode> representing the parsed MainFuncDef
     */
    Optional<ASTNode> parseMainFuncDef();

    /**
     * FuncType → 'void' | 'int'
     *
     * @return Optional<ASTNode> representing the parsed FuncType
     */
    Optional<ASTNode> parseFuncType();

    /**
     * FuncFParams → FuncFParam { ',' FuncFParam }
     *
     * @return Optional<ASTNode> representing the parsed FuncFParams
     */
    Optional<ASTNode> parseFuncFParams();

    /**
     * FuncFParam → BType Ident ['[' ']' { '[' ConstExp ']' }]
     *
     * @return Optional<ASTNode> representing the parsed FuncFParam
     */
    Optional<ASTNode> parseFuncFParam();

    /**
     * Block → '{' { BlockItem } '}'
     *
     * @return Optional<ASTNode> representing the parsed Block
     */
    Optional<ASTNode> parseBlock();

    /**
     * BlockItem → Decl | Stmt
     *
     * @return Optional<ASTNode> representing the parsed BlockItem
     */
    Optional<ASTNode> parseBlockItem();

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
    Optional<ASTNode> parseStmt();

    /**
     * ForStmt → LVal '=' Exp
     *
     * @return Optional<ASTNode> representing the parsed ForStmt
     */
    Optional<ASTNode> parseForStmt();

    /**
     * Exp → AddExp
     *
     * @return Optional<ASTNode> representing the parsed Exp
     */
    Optional<ASTNode> parseExp();

    /**
     * Cond → LOrExp
     *
     * @return Optional<ASTNode> representing the parsed Cond
     */
    Optional<ASTNode> parseCond();

    /**
     * LVal → Ident {'[' Exp ']'}
     *
     * @return Optional<ASTNode> representing the parsedLVal
     */
    Optional<ASTNode> parseLVal();

    /**
     * PrimaryExp → '(' Exp ')' | LVal | Number
     *
     * @return Optional<ASTNode> representing the parsed PrimaryExp
     */
    Optional<ASTNode> parsePrimaryExp();

    /**
     * Number → IntConst
     *
     * @return Optional<ASTNode> representing the parsed Number
     */
    Optional<ASTNode> parseNumber();

    /**
     * UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
     *
     * @return Optional<ASTNode> representing the parsed UnaryExp
     */
    Optional<ASTNode> parseUnaryExp();

    /**
     * UnaryOp → '+' | '−' | '!'
     *
     * @return Optional<ASTNode> representing the parsed parseUnaryOp
     */
    Optional<ASTNode> parseUnaryOp();

    /**
     * FuncRParams → Exp { ',' Exp }
     *
     * @return Optional representing the parsed FuncRParams
     */
    Optional<ASTNode> parseFuncRParams();

    /**
     * MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
     *
     * @return Optional<ASTNode> representing the parsed MulExp
     */
    Optional<ASTNode> parseMulExp();

    /**
     * AddExp → MulExp | AddExp ('+' | '−') MulExp
     *
     * @return Optional<ASTNode> representing the parsed AddExp
     */
    Optional<ASTNode> parseAddExp();

    /**
     * RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
     *
     * @return Optional representing the parsed RelExp
     */
    Optional<ASTNode> parseRelExp();

    /**
     * EqExp → RelExp | EqExp ('==' | '!=') RelExp
     *
     * @return Optional representing the parsed EqExp
     */
    Optional<ASTNode> parseEqExp();

    /**
     * TypeSpec → BType | 'void'
     *
     * @return Optional<ASTNode> representing the parsed TypeSpec
     */
    Optional<ASTNode> parseTypeSpec();


    /**
     * LAndExp → EqExp | LAndExp '&&' EqExp
     *
     * @return Optional<ASTNode> representing the parsed LAndExp
     */
    Optional<ASTNode> parseLAndExp();

    /**
     * LOrExp → LAndExp | LOrExp '||' LAndExp
     *
     * @return Optional<ASTNode> representing the parsed LOrExp
     */
    Optional<ASTNode> parseLOrExp();

    /**
     * ConstExp → AddExp
     *
     * @return Optional<ASTNode> representing the parsed ConstExp
     */
    Optional<ASTNode> parseConstExp();


    /**
     * Parse the terminals. Include `keyword, punctuation, operator, string, number`.
     *
     * @return Optional ASTLeaf that been parsed.
     */
    Optional<ASTLeaf> parseTerminal();

    /**
     * Parse the terminals. Include `keyword, punctuation, operator, string, number`.
     *
     * @return Optional ASTLeaf that been parsed.
     */
    Optional<ASTLeaf> parseTerminal(GrammarType... type);
}