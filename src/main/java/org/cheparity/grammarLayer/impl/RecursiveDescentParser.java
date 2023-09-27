package grammarLayer.impl;

import exception.NotATerminalException;
import grammarLayer.GrammarParser;
import grammarLayer.dataStruct.ASTLeaf;
import grammarLayer.dataStruct.ASTNode;
import grammarLayer.dataStruct.GrammarType;
import lexLayer.LexicalParser;
import lexLayer.dataStruct.Token;
import lexLayer.impl.LexicalParserImpl;
import utils.LoggerUtil;

import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Logger;

public class RecursiveDescentParser implements GrammarParser {
    private final static Logger LOGGER = LoggerUtil.getLogger();
    private final LexicalParser lexicalParser = LexicalParserImpl.getInstance();
    private final ArrayList<Token> tokens = lexicalParser.getAllTokens();
    private ASTNode AST;
    private int nowIndex = 0;
    private int initIndex;
    private Token nowToken;

    private void error() {
        LOGGER.severe("Error");
    }

    private void next() {
        if (nowIndex < tokens.size() - 1) {
            nowIndex++;
            nowToken = tokens.get(nowIndex);
        } else {
            LOGGER.info("End of tokens");
        }
    }

    private ASTNode begin(GrammarType type) {
        initIndex = nowIndex;
        return new ASTNode(type);
    }

    private Optional<ASTNode> failed() {
        nowIndex = initIndex;
        nowToken = tokens.get(nowIndex);
        return Optional.empty();
    }

    private Optional<ASTNode> done(ASTNode ast) {
        nowIndex++;
        nowToken = tokens.get(nowIndex);
        return Optional.of(ast);
    }


    /**
     * CompUnit → {Decl} {FuncDef} MainFuncDef
     *
     * @return Optional<ASTNode> representing the parsed CompUnit
     */
    @Override
    public Optional<ASTNode> parseCompUnit() {
        //todo sure has bugs
        ASTNode compUnit = new ASTNode(GrammarType.COMP_UNIT);
        Optional<ASTNode> decl, funcDef, mainFuncDef;
        for (decl = parseDecl(); decl.isPresent(); ) {
            compUnit.addChild(decl.get());
        }
        for (funcDef = parseFuncDef(); funcDef.isPresent(); ) {
            compUnit.addChild(funcDef.get());
        }
        mainFuncDef = parseMainFuncDef();
        mainFuncDef.ifPresentOrElse(compUnit::addChild, this::error);
        return done(compUnit);
    }

    /**
     * Decl → ConstDecl | VarDecl
     *
     * @return Optional<ASTNode> representing the parsed Decl
     */
    @Override
    public Optional<ASTNode> parseDecl() {
        ASTNode decl = begin(GrammarType.DECL);
        var constDecl = parseConstDecl();
        if (constDecl.isPresent()) {
            decl.addChild(constDecl.get());
            return done(decl);
        } else {
            failed();
        }

        var varDecl = parseVarDecl();
        if (varDecl.isPresent()) {
            decl.addChild(varDecl.get());
            return done(decl);
        }
        return failed();
    }

    /**
     * ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
     *
     * @return Optional<ASTNode> representing the parsed ConstDecl
     */
    @Override
    public Optional<ASTNode> parseConstDecl() {
        ASTNode constDecl = begin(GrammarType.CONST_DECL);
        Optional<ASTLeaf> constTk = parseTerminal(GrammarType.CONST);
        if (constTk.isPresent()) {
            constDecl.addChild(constTk.get());
        } else {
            return failed();
        }

        Optional<ASTNode> bType = parseBType();
//        bType.ifPresentOrElse(constDecl::addChild, this::failed);
        if (bType.isPresent()) {
            constDecl.addChild(bType.get());
        } else {
            return failed();
        }


        Optional<ASTNode> constDef = parseConstDef();
        if (constDef.isPresent()) {
            constDecl.addChild(constDef.get());
        } else {
            return failed();
        }

        for (var comma = parseTerminal(GrammarType.COMMA); comma.isPresent(); ) {
            constDecl.addChild(comma.get());
            constDef = parseConstDef();
            if (constDef.isPresent()) {
                constDecl.addChild(constDef.get());
            } else {
                return failed();
            }
        }

        Optional<ASTLeaf> semicolon = parseTerminal(GrammarType.SEMICOLON);
        if (semicolon.isPresent()) {
            constDecl.addChild(semicolon.get());
            return done(constDecl);
        }

        return failed();
    }

    /**
     * BType → 'int'
     *
     * @return Optional<ASTNode> representing the parsed BType
     */
    @Override
    public Optional<ASTNode> parseBType() {
        ASTNode bType = begin(GrammarType.B_TYPE);
        Optional<ASTLeaf> intTk = parseTerminal(GrammarType.INT);
        if (intTk.isPresent()) {
            bType.addChild(intTk.get());
            return done(bType);
        }

        return failed();
    }

    /**
     * ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
     *
     * @return Optional<ASTNode> representing the parsed ConstDef
     */
    @Override
    public Optional<ASTNode> parseConstDef() {
        ASTNode constDef = begin(GrammarType.CONST_DEF);

        //Ident
        Optional<ASTLeaf> ident = parseTerminal(GrammarType.IDENTIFIER);
        if (ident.isPresent()) {
            constDef.addChild(ident.get());
        } else {
            return failed();
        }

        // '[' ConstExp ']'
        for (var leftBracket = parseTerminal(GrammarType.LEFT_BRACKET); leftBracket.isPresent(); ) {
            constDef.addChild(leftBracket.get());
            Optional<ASTNode> constExp = parseConstExp();
            if (constExp.isPresent()) {
                constDef.addChild(constExp.get());
            } else {
                return failed();
            }
            Optional<ASTLeaf> rightBracket = parseTerminal(GrammarType.RIGHT_BRACKET);
            if (rightBracket.isPresent()) {
                constDef.addChild(rightBracket.get());
            } else {
                return failed();
            }
        }

        return done(constDef);
    }

    /**
     * ConstInitVal → ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
     *
     * @return Optional<ASTNode> representing the parsed ConstInitVal
     */
    @Override
    public Optional<ASTNode> parseConstInitVal() {
        ASTNode constInitVal = begin(GrammarType.CONST_INIT_VAL);

        Optional<ASTNode> constExp = parseConstExp();
        if (constExp.isPresent()) {
            constInitVal.addChild(constExp.get());
            return done(constInitVal);
        } else {
            failed();
        }

        // '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
        Optional<ASTLeaf> leftBrace = parseTerminal(GrammarType.LEFT_BRACE);
        if (leftBrace.isPresent()) {
            constInitVal.addChild(leftBrace.get());
            Optional<ASTNode> constInitVal1 = parseConstInitVal();
            if (constInitVal1.isPresent()) {
                constInitVal.addChild(constInitVal1.get());
            } else {
                failed();
            }

            for (var comma = parseTerminal(GrammarType.COMMA); comma.isPresent(); ) {
                constInitVal.addChild(comma.get());
                constInitVal1 = parseConstInitVal();
                if (constInitVal1.isPresent()) {
                    constInitVal.addChild(constInitVal1.get());
                } else {
                    failed();
                }
            }

            Optional<ASTLeaf> rightBrace = parseTerminal(GrammarType.RIGHT_BRACE);
            if (rightBrace.isPresent()) {
                constInitVal.addChild(rightBrace.get());
                return done(constInitVal);
            }
        }

        return done(constInitVal);
    }

    /**
     * VarDecl → BType VarDef { ',' VarDef } ';'
     *
     * @return Optional<ASTNode> representing the parsed VarDecl
     */
    @Override
    public Optional<ASTNode> parseVarDecl() {
        var varDecl = begin(GrammarType.B_TYPE);
        Optional<ASTNode> bType = parseBType();
        if (bType.isPresent()) {
            varDecl.addChild(bType.get());
        } else {
            return failed();
        }

        Optional<ASTNode> varDef = parseVarDef();
        if (varDef.isPresent()) {
            varDecl.addChild(varDef.get());
        } else {
            return failed();
        }

        for (var comma = parseTerminal(GrammarType.COMMA); comma.isPresent(); ) {
            varDecl.addChild(comma.get());
            varDef = parseVarDef();
            if (varDef.isPresent()) {
                varDecl.addChild(varDef.get());
            } else {
                return failed();
            }
        }

        return done(varDecl);
    }

    /**
     * VarDef → Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal
     *
     * @return Optional<ASTNode> representing the parsed VarDef
     */
    @Override
    public Optional<ASTNode> parseVarDef() {
        ASTNode varDef = begin(GrammarType.VAR_DEF);
        Optional<ASTNode> constExp, initVal;
        //ident(must)
        Optional<ASTLeaf> ident;
        if ((ident = parseTerminal(GrammarType.IDENTIFIER)).isPresent()) {
            varDef.addChild(ident.get());
        } else {
            return failed();
        }
        // { '[' ConstExp ']' } optional
        for (var leftBracket = parseTerminal(GrammarType.LEFT_BRACKET); leftBracket.isPresent(); ) {
            varDef.addChild(leftBracket.get());
            constExp = parseConstExp();
            if (constExp.isPresent()) {
                varDef.addChild(constExp.get());
            } else {
                return failed();
            }
            Optional<ASTLeaf> rightBracket = parseTerminal(GrammarType.RIGHT_BRACKET);
            if (rightBracket.isPresent()) {
                varDef.addChild(rightBracket.get());
            } else {
                return failed();
            }
        }

        // '=' InitVal optional
        Optional<ASTLeaf> assign;
        if ((assign = parseTerminal(GrammarType.ASSIGN)).isPresent()) {
            varDef.addChild(assign.get());
            initVal = parseInitVal();
            if (initVal.isPresent()) {
                varDef.addChild(initVal.get());
            } else {
                return failed();
            }
        }

        return done(varDef);

    }

    /**
     * InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'
     *
     * @return Optional<ASTNode> representing the parsed InitVal
     */
    @Override
    public Optional<ASTNode> parseInitVal() {
        var initVal = begin(GrammarType.INIT_VAL);
        Optional<ASTNode> exp, initVal1;
        Optional<ASTLeaf> leftBrace, comma, rightBrace;
        //Exp
        if ((exp = parseExp()).isPresent()) {
            initVal.addChild(exp.get());
            return done(initVal);
        } else {
            failed();
        }

        // '{' [ InitVal { ',' InitVal } ] '}'
        if ((leftBrace = parseTerminal(GrammarType.LEFT_BRACE)).isPresent()) {
            initVal.addChild(leftBrace.get());
            initVal1 = parseInitVal();
            if (initVal1.isPresent()) {
                initVal.addChild(initVal1.get());
            } else {
                failed();
            }

            for (comma = parseTerminal(GrammarType.COMMA); comma.isPresent(); ) {
                initVal.addChild(comma.get());
                initVal1 = parseInitVal();
                if (initVal1.isPresent()) {
                    initVal.addChild(initVal1.get());
                } else {
                    return failed();
                }
            }

            if ((rightBrace = parseTerminal(GrammarType.RIGHT_BRACE)).isPresent()) {
                initVal.addChild(rightBrace.get());
                return done(initVal);
            } else return failed();
        }

        return done(initVal);
    }

    /**
     * FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
     *
     * @return Optional<ASTNode> representing the parsed FuncDef
     */
    @Override
    public Optional<ASTNode> parseFuncDef() {
        ASTNode funcDef = begin(GrammarType.FUNC_DEF);
        Optional<ASTNode> funcType = parseFuncType();
        if (funcType.isPresent()) {
            funcDef.addChild(funcType.get());
        } else {
            return failed();
        }

        Optional<ASTLeaf> ident = parseTerminal(GrammarType.IDENTIFIER);
        if (ident.isPresent()) {
            funcDef.addChild(ident.get());
        } else {
            return failed();
        }

        Optional<ASTLeaf> leftParen = parseTerminal(GrammarType.LEFT_PAREN);
        if (leftParen.isPresent()) {
            funcDef.addChild(leftParen.get());
        } else {
            return failed();
        }

        parseFuncFParams().ifPresent(funcDef::addChild); //optional

        Optional<ASTLeaf> rightParen = parseTerminal(GrammarType.RIGHT_PAREN);
        if (rightParen.isPresent()) {
            funcDef.addChild(rightParen.get());
        } else {
            return failed();
        }

        Optional<ASTNode> block = parseBlock();
        if (block.isPresent()) {
            funcDef.addChild(block.get());
        } else {
            return failed();
        }

        return done(funcDef);
    }

    /**
     * MainFuncDef → 'int' 'main' '(' ')' Block
     *
     * @return Optional<ASTNode> representing the parsed MainFuncDef
     */
    @Override
    public Optional<ASTNode> parseMainFuncDef() {
        ASTNode mainFuncDef = begin(GrammarType.MAIN_FUNC_DEF);
        Optional<ASTLeaf> intTk = parseTerminal(GrammarType.INT);
        if (intTk.isPresent()) {
            mainFuncDef.addChild(intTk.get());
        } else {
            return failed();
        }

        Optional<ASTLeaf> mainTk = parseTerminal(GrammarType.MAIN_FUNC_DEF);
        if (mainTk.isPresent()) {
            mainFuncDef.addChild(mainTk.get());
        } else {
            return failed();
        }

        Optional<ASTLeaf> leftParen = parseTerminal(GrammarType.LEFT_PAREN);
        if (leftParen.isPresent()) {
            mainFuncDef.addChild(leftParen.get());
        } else {
            return failed();
        }

        Optional<ASTLeaf> rightParen = parseTerminal(GrammarType.RIGHT_PAREN);
        if (rightParen.isPresent()) {
            mainFuncDef.addChild(rightParen.get());
        } else {
            return failed();
        }

        Optional<ASTNode> block = parseBlock();
        if (block.isPresent()) {
            mainFuncDef.addChild(block.get());
        } else {
            return failed();
        }

        return done(mainFuncDef);

    }

    /**
     * FuncType → 'void' | 'int'
     *
     * @return Optional<ASTNode> representing the parsed FuncType
     */
    @Override
    public Optional<ASTNode> parseFuncType() {
        ASTNode funcType = begin(GrammarType.FUNC_TYPE);
        Optional<ASTLeaf> voidTk = parseTerminal(GrammarType.VOID);
        if (voidTk.isPresent()) {
            funcType.addChild(voidTk.get());
            return done(funcType);
        }

        Optional<ASTLeaf> intTk = parseTerminal(GrammarType.INT);
        if (intTk.isPresent()) {
            funcType.addChild(intTk.get());
            return done(funcType);
        }

        return failed();
    }

    /**
     * FuncFParams → FuncFParam { ',' FuncFParam }
     *
     * @return Optional<ASTNode> representing the parsed FuncFParams
     */
    @Override
    public Optional<ASTNode> parseFuncFParams() {
        ASTNode funcFParams = begin(GrammarType.FUNC_FPARAMS);
        Optional<ASTNode> funcFParam = parseFuncFParam();
        if (funcFParam.isPresent()) {
            funcFParams.addChild(funcFParam.get());
        } else {
            return failed();
        }

        for (var comma = parseTerminal(GrammarType.COMMA); comma.isPresent(); ) {
            funcFParams.addChild(comma.get());
            funcFParam = parseFuncFParam();
            if (funcFParam.isPresent()) {
                funcFParams.addChild(funcFParam.get());
            } else {
                return failed();
            }
        }

        return done(funcFParams);
    }

    /**
     * FuncFParam → BType Ident ['[' ']' { '[' ConstExp ']' }]
     *
     * @return Optional<ASTNode> representing the parsed FuncFParam
     */
    @Override
    public Optional<ASTNode> parseFuncFParam() {
        var funcFParam = begin(GrammarType.FUNC_FPARAM);
        Optional<ASTNode> bType = parseBType();
        if (bType.isPresent()) {
            funcFParam.addChild(bType.get());
        } else {
            return failed();
        }

        Optional<ASTLeaf> ident = parseTerminal(GrammarType.IDENTIFIER);
        if (ident.isPresent()) {
            funcFParam.addChild(ident.get());
        } else {
            return failed();
        }

        for (var leftBracket = parseTerminal(GrammarType.LEFT_BRACKET); leftBracket.isPresent(); ) {
            funcFParam.addChild(leftBracket.get());
            Optional<ASTLeaf> rightBracket = parseTerminal(GrammarType.RIGHT_BRACKET);
            if (rightBracket.isPresent()) {
                funcFParam.addChild(rightBracket.get());
            } else {
                return failed();
            }
            for (leftBracket = parseTerminal(GrammarType.LEFT_BRACKET); leftBracket.isPresent(); ) {
                funcFParam.addChild(leftBracket.get());
                Optional<ASTNode> constExp = parseConstExp();
                if (constExp.isPresent()) {
                    funcFParam.addChild(constExp.get());
                } else {
                    return failed();
                }
                rightBracket = parseTerminal(GrammarType.RIGHT_BRACKET);
                if (rightBracket.isPresent()) {
                    funcFParam.addChild(rightBracket.get());
                } else {
                    return failed();
                }
            }
        }

        return done(funcFParam);
    }

    /**
     * Block → '{' { BlockItem } '}'
     *
     * @return Optional<ASTNode> representing the parsed Block
     */
    @Override
    public Optional<ASTNode> parseBlock() {
        var Block = begin(GrammarType.BLOCK);
        Optional<ASTLeaf> leftBrace = parseTerminal(GrammarType.LEFT_BRACE);
        if (leftBrace.isPresent()) {
            Block.addChild(leftBrace.get());
        } else {
            return failed();
        }

        Optional<ASTNode> blockItem;
        for (blockItem = parseBlockItem(); blockItem.isPresent(); ) {
            Block.addChild(blockItem.get());
        }

        Optional<ASTLeaf> rightBrace = parseTerminal(GrammarType.RIGHT_BRACE);
        if (rightBrace.isPresent()) {
            Block.addChild(rightBrace.get());
        } else {
            return failed();
        }

        return done(Block);
    }

    /**
     * BlockItem → Decl | Stmt
     *
     * @return Optional<ASTNode> representing the parsed BlockItem
     */
    @Override
    public Optional<ASTNode> parseBlockItem() {
        var BlockItem = begin(GrammarType.BLOCK_ITEM);
        Optional<ASTNode> decl = parseDecl();
        if (decl.isPresent()) {
            BlockItem.addChild(decl.get());
            return done(BlockItem);
        }

        Optional<ASTNode> stmt = parseStmt();
        if (stmt.isPresent()) {
            BlockItem.addChild(stmt.get());
            return done(BlockItem);
        }

        return failed();
    }

    /**
     * Stmt → LVal '=' Exp ';'
     * | LVal '=' 'getint''('')'';'
     * | [Exp] ';'
     * | Block
     * | 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
     * | 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
     * | 'break' ';'
     * | 'continue' ';'
     * | 'return' [Exp] ';'
     * | 'printf''('FormatString{','Exp}')'';'
     *
     * @return Optional<ASTNode> representing the parsed Stmt
     */
    @Override
    public Optional<ASTNode> parseStmt() {
        var Stmt = begin(GrammarType.STMT);
        Optional<ASTNode> lVal, exp, block;
        Optional<ASTLeaf> semicolon, leftParen, rightParen, getintTk, assign, keyword;

        //LVal '=' Exp ';' | LVal '=' 'getint''('')'';'
        lVal = parseLVal();
        if (lVal.isPresent()) {
            Stmt.addChild(lVal.get());
            assign = parseTerminal(GrammarType.ASSIGN);
            if (assign.isPresent()) {
                Stmt.addChild(assign.get());
            } else {
                return failed();
            }
            //case 1: exp
            exp = parseExp();
            if (exp.isPresent()) {
                Stmt.addChild(exp.get());
                semicolon = parseTerminal(GrammarType.SEMICOLON);
                if (semicolon.isPresent()) {
                    Stmt.addChild(semicolon.get());
                    return done(Stmt);
                } else {
                    return failed();
                }
            }

            //case 2: getint()
            getintTk = parseTerminal(GrammarType.GETINT);
            if (getintTk.isPresent()) {
                Stmt.addChild(getintTk.get());
                leftParen = parseTerminal(GrammarType.LEFT_PAREN);
                if (leftParen.isPresent()) {
                    Stmt.addChild(leftParen.get());
                } else {
                    return failed();
                }
                rightParen = parseTerminal(GrammarType.RIGHT_PAREN);
                if (rightParen.isPresent()) {
                    Stmt.addChild(rightParen.get());
                } else {
                    return failed();
                }
                semicolon = parseTerminal(GrammarType.SEMICOLON);
                if (semicolon.isPresent()) {
                    Stmt.addChild(semicolon.get());
                    return done(Stmt);
                } else {
                    return failed();
                }
            }

        }

        // [Exp] ';'
        exp = parseExp();
        if (exp.isPresent()) {
            Stmt.addChild(exp.get());

            semicolon = parseTerminal(GrammarType.SEMICOLON);
            if (semicolon.isPresent()) {
                Stmt.addChild(semicolon.get());
                return done(Stmt);
            } else {
                return failed();
            }
        }

        //Block
        block = parseBlock();
        if (block.isPresent()) {
            Stmt.addChild(block.get());
            return done(Stmt);
        }

        keyword = parseTerminal(); //todo change this
        if (keyword.isEmpty()) {
            return failed();
        }

        var grammarType = keyword.get().getGrammarType();
        if (grammarType.equals(GrammarType.IF)) {
            leftParen = parseTerminal(GrammarType.LEFT_PAREN);
            if (leftParen.isPresent()) {
                Stmt.addChild(leftParen.get());
            } else {
                return failed();
            }
            var cond = parseCond();
            if (cond.isPresent()) {
                Stmt.addChild(cond.get());
            } else {
                return failed();
            }
            rightParen = parseTerminal(GrammarType.RIGHT_PAREN);
            if (rightParen.isPresent()) {
                Stmt.addChild(rightParen.get());
            } else {
                return failed();
            }

            var stmt1 = parseStmt();
            if (stmt1.isPresent()) {
                Stmt.addChild(stmt1.get());
            } else {
                return failed();
            }
            var elseTk = parseTerminal(GrammarType.ELSE);
            if (elseTk.isPresent()) {
                Stmt.addChild(elseTk.get());
                var stmt2 = parseStmt();
                if (stmt2.isPresent()) {
                    Stmt.addChild(stmt2.get());
                } else {
                    return failed();
                }
            }
            return done(Stmt);
        }
        //'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
        else if (grammarType.equals(GrammarType.FOR)) {
            leftParen = parseTerminal(GrammarType.LEFT_PAREN);
            if (leftParen.isPresent()) {
                Stmt.addChild(leftParen.get());
            } else {
                return failed();
            }

            parseForStmt().ifPresent(Stmt::addChild);

            var semicolon1 = parseTerminal(GrammarType.SEMICOLON);
            if (semicolon1.isPresent()) {
                Stmt.addChild(semicolon1.get());
            } else {
                return failed();
            }

            parseCond().ifPresent(Stmt::addChild);

            var semicolon2 = parseTerminal(GrammarType.SEMICOLON);
            if (semicolon2.isPresent()) {
                Stmt.addChild(semicolon2.get());
            } else {
                return failed();
            }

            parseForStmt().ifPresent(Stmt::addChild);

            var stmt = parseStmt();
            if (stmt.isPresent()) {
                Stmt.addChild(stmt.get());
                return done(Stmt);
            } else {
                return failed();
            }
        }
        //'break' ';'
        else if (grammarType.equals(GrammarType.BREAK)) {
            semicolon = parseTerminal(GrammarType.SEMICOLON);
            if (semicolon.isPresent()) {
                Stmt.addChild(semicolon.get());
                return done(Stmt);
            } else {
                return failed();
            }
        }
        //'continue' ';'
        else if (grammarType.equals(GrammarType.CONTINUE)) {
            semicolon = parseTerminal(GrammarType.SEMICOLON);
            if (semicolon.isPresent()) {
                Stmt.addChild(semicolon.get());
                return done(Stmt);
            } else {
                return failed();
            }
        }
        //'return' [Exp] ';'
        else if (grammarType.equals(GrammarType.RETURN)) {
            parseExp().ifPresent(Stmt::addChild);
            semicolon = parseTerminal(GrammarType.SEMICOLON);
            if (semicolon.isPresent()) {
                Stmt.addChild(semicolon.get());
                return done(Stmt);
            } else {
                return failed();
            }
        }
        //'printf''('FormatString{','Exp}')'';'
        else if (grammarType.equals(GrammarType.PRINTF)) {
            leftParen = parseTerminal(GrammarType.LEFT_PAREN);
            if (leftParen.isPresent()) {
                Stmt.addChild(leftParen.get());
            } else {
                return failed();
            }

            var formatString = parseTerminal(GrammarType.FORMAT_STRING);
            if (formatString.isPresent()) {
                Stmt.addChild(formatString.get());
            } else {
                return failed();
            }

            for (var comma = parseTerminal(GrammarType.COMMA); comma.isPresent(); ) {
                Stmt.addChild(comma.get());
                var exp1 = parseExp();
                exp1.ifPresentOrElse(Stmt::addChild, this::failed);
                if (exp1.isPresent()) {
                    Stmt.addChild(exp1.get());
                } else {
                    return failed();
                }
            }
            return done(Stmt);
        }
        return failed();
    }

    /**
     * ForStmt → LVal '=' Exp
     *
     * @return Optional<ASTNode> representing the parsed ForStmt
     */
    @Override
    public Optional<ASTNode> parseForStmt() {
        Optional<ASTNode> lVal = parseLVal();
        if (lVal.isEmpty()) {
            return failed();
        }

        Optional<ASTLeaf> assign = parseTerminal(GrammarType.ASSIGN);
        if (assign.isPresent()) {
            lVal.get().addChild(assign.get());
        } else {
            return failed();
        }

        Optional<ASTNode> exp = parseExp();
        if (exp.isPresent()) {
            lVal.get().addChild(exp.get());
            return done(lVal.get());
        }
        return failed();
    }

    /**
     * Exp → AddExp
     *
     * @return Optional<ASTNode> representing the parsed Exp
     */
    @Override
    public Optional<ASTNode> parseExp() {
        ASTNode exp = begin(GrammarType.EXP);
        var addExp = parseAddExp();
        if (addExp.isPresent()) {
            exp.addChild(addExp.get());
            return done(exp);
        }
        return failed();
    }

    /**
     * Cond → LOrExp
     *
     * @return Optional<ASTNode> representing the parsed Cond
     */
    @Override
    public Optional<ASTNode> parseCond() {
        ASTNode cond = begin(GrammarType.COND);
        var LorExp = parseLOrExp();
        if (LorExp.isPresent()) {
            cond.addChild(LorExp.get());
            return done(cond);
        }
        return failed();
    }

    /**
     * LVal → Ident {'[' Exp ']'}
     *
     * @return Optional<ASTNode> representing the parsedLVal
     */
    @Override
    public Optional<ASTNode> parseLVal() {
        ASTNode LVal = begin(GrammarType.LVAL);
        Optional<ASTLeaf> ident = parseTerminal(GrammarType.IDENTIFIER);
        if (ident.isPresent()) {
            LVal.addChild(ident.get());
        } else {
            return failed();
        }
        return done(LVal);
    }

    /**
     * PrimaryExp →  LVal | Number | '(' Exp ')'
     *
     * @return Optional<ASTNode> representing the parsed PrimaryExp
     */
    @Override
    public Optional<ASTNode> parsePrimaryExp() {
        ASTNode primaryExp = begin(GrammarType.PRIMARY_EXP);

        var lVal = parseLVal();
        if (lVal.isPresent()) {
            primaryExp.addChild(lVal.get());
            return done(primaryExp);
        }

        var number = parseNumber();
        if (number.isPresent()) {
            primaryExp.addChild(number.get());
            return done(primaryExp);
        }

        var leftParen = parseTerminal(GrammarType.LEFT_PAREN);
        if (leftParen.isEmpty()) return failed();
        primaryExp.addChild(leftParen.get());

        var exp = parseExp();
        if (exp.isEmpty()) return failed();
        primaryExp.addChild(exp.get());

        var rightParen = parseTerminal(GrammarType.RIGHT_PAREN);
        if (rightParen.isEmpty()) return failed();
        primaryExp.addChild(rightParen.get());

        return done(primaryExp);
    }

    /**
     * Number → IntConst
     *
     * @return Optional<ASTNode> representing the parsed Number
     */
    @Override
    public Optional<ASTNode> parseNumber() {
        var number = begin(GrammarType.NUMBER);
        var intConst = parseTerminal(GrammarType.INT_CONST);
        if (intConst.isPresent()) {
            number.addChild(intConst.get());
            return done(number);
        }

        return failed();
    }

    /**
     * UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
     *
     * @return Optional<ASTNode> representing the parsed UnaryExp
     */
    @Override
    public Optional<ASTNode> parseUnaryExp() {
        var UnaryExp = begin(GrammarType.UNARY_EXP);
        //PrimaryExp
        var primaryExp = parsePrimaryExp();
        if (primaryExp.isPresent()) {
            UnaryExp.addChild(primaryExp.get());
            return done(UnaryExp);
        }
        // Ident '(' [FuncRParams] ')'
        var ident = parseTerminal(GrammarType.IDENTIFIER);
        if (ident.isPresent()) {
            UnaryExp.addChild(ident.get());

            var leftParen = parseTerminal(GrammarType.LEFT_PAREN);
            if (leftParen.isPresent()) {
                UnaryExp.addChild(leftParen.get());
                parseFuncRParams().ifPresent(UnaryExp::addChild);
                var rightParen = parseTerminal(GrammarType.RIGHT_PAREN);
                if (rightParen.isPresent()) {
                    UnaryExp.addChild(rightParen.get());
                    return done(UnaryExp);
                }
            }
        }
        // UnaryOp UnaryExp
        var unaryOp = parseUnaryOp();
        if (unaryOp.isPresent()) {
            UnaryExp.addChild(unaryOp.get());
            var unaryExp = parseUnaryExp();
            if (unaryExp.isPresent()) {
                UnaryExp.addChild(unaryExp.get());
                return done(UnaryExp);
            }
        }

        return failed();
    }

    /**
     * UnaryOp → '+' | '−' | '!'
     *
     * @return Optional<ASTNode> representing the parsed parseUnaryOp
     */
    @Override
    public Optional<ASTNode> parseUnaryOp() {
        var unaryOp = begin(GrammarType.UNARY_OP);
        var plus = parseTerminal(GrammarType.PLUS);
        if (plus.isPresent()) {
            unaryOp.addChild(plus.get());
            return done(unaryOp);
        }

        var minus = parseTerminal(GrammarType.MINUS);
        if (minus.isPresent()) {
            unaryOp.addChild(minus.get());
            return done(unaryOp);
        }

        var not = parseTerminal(GrammarType.NOT);
        if (not.isPresent()) {
            unaryOp.addChild(not.get());
            return done(unaryOp);
        }

        return failed();
    }

    /**
     * FuncRParams → Exp { ',' Exp }
     *
     * @return Optional representing the parsed FuncRParams
     */
    @Override
    public Optional<ASTNode> parseFuncRParams() {
        var funcRParams = begin(GrammarType.FUNC_RPARAMS);
        var exp = parseExp();
        if (exp.isPresent()) {
            funcRParams.addChild(exp.get());
            for (var comma = parseTerminal(GrammarType.COMMA); comma.isPresent(); ) {
                funcRParams.addChild(comma.get());
                exp = parseExp();
                if (exp.isPresent()) {
                    funcRParams.addChild(exp.get());
                } else {
                    return failed();
                }
            }
            return done(funcRParams);
        }

        return failed();
    }

    /**
     * MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
     *
     * @return Optional<ASTNode> representing the parsed MulExp
     */
    @Override
    public Optional<ASTNode> parseMulExp() {
        var mulExp = begin(GrammarType.MUL_EXP);
        var unaryExp = parseUnaryExp();
        if (unaryExp.isPresent()) {
            mulExp.addChild(unaryExp.get());
            return done(mulExp);
        }

        var mulOp = parseTerminal(GrammarType.MULTIPLY, GrammarType.DIVIDE, GrammarType.MOD);
        if (mulOp.isPresent()) {
            mulExp.addChild(mulOp.get());
            unaryExp = parseUnaryExp();
            if (unaryExp.isPresent()) {
                mulExp.addChild(unaryExp.get());
                return done(mulExp);
            }
        }

        return failed();
    }

    /**
     * AddExp → MulExp | AddExp ('+' | '−') MulExp
     *
     * @return Optional<ASTNode> representing the parsed AddExp
     */
    @Override
    public Optional<ASTNode> parseAddExp() {
        var addExp = begin(GrammarType.ADD_EXP);

        var mulExp = parseMulExp();
        if (mulExp.isPresent()) {
            addExp.addChild(mulExp.get());
            return done(addExp);
        }

        var addExp2 = parseAddExp();
        if (addExp2.isPresent()) {
            addExp.addChild(addExp2.get());
            var addOp = parseTerminal(GrammarType.PLUS, GrammarType.MINUS);
            if (addOp.isPresent()) {
                addExp.addChild(addOp.get());
                mulExp = parseMulExp();
                if (mulExp.isPresent()) {
                    addExp.addChild(mulExp.get());
                    return done(addExp);
                }
            }
        }

        return failed();
    }

    /**
     * RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
     *
     * @return Optional representing the parsed RelExp
     */
    @Override
    public Optional<ASTNode> parseRelExp() {
        var relExp = begin(GrammarType.REL_EXP);
        

        return failed();
    }

    /**
     * EqExp → RelExp | EqExp ('==' | '!=') RelExp
     *
     * @return Optional representing the parsed EqExp
     */
    @Override
    public Optional<ASTNode> parseEqExp() {
        return failed();
    }

    /**
     * TypeSpec → BType | 'void'
     *
     * @return Optional<ASTNode> representing the parsed TypeSpec
     */
    @Override
    public Optional<ASTNode> parseTypeSpec() {
        return failed();
    }

    /**
     * LAndExp → EqExp | LAndExp '&&' EqExp
     *
     * @return Optional<ASTNode> representing the parsed LAndExp
     */
    @Override
    public Optional<ASTNode> parseLAndExp() {
        return failed();
    }

    /**
     * LOrExp → LAndExp | LOrExp '||' LAndExp
     *
     * @return Optional<ASTNode> representing the parsed LOrExp
     */
    @Override
    public Optional<ASTNode> parseLOrExp() {
        return failed();
    }

    /**
     * ConstExp → AddExp
     *
     * @return Optional<ASTNode> representing the parsed ConstExp
     */
    @Override
    public Optional<ASTNode> parseConstExp() {
        return failed();
    }

    /**
     * Parse the terminals. Include `keyword, punctuation, operator, string, number`.
     *
     * @return Optional ASTLeaf that been parsed.
     */
    @Override
    public Optional<ASTLeaf> parseTerminal() {
        ASTLeaf astLeaf;
        try {
            astLeaf = new ASTLeaf(nowToken);
        } catch (NotATerminalException e) {
            return Optional.empty();
        }
        return Optional.of(astLeaf);
    }

    /**
     * Parse the terminals. Include `keyword, punctuation, operator, string, number`.
     *
     * @return Optional ASTLeaf that been parsed.
     */
    @Override
    public Optional<ASTLeaf> parseTerminal(GrammarType... type) {
        ASTLeaf astLeaf;
        Optional<ASTLeaf> terminal = parseTerminal();
        if (terminal.isPresent()) {
            for (var t : type) {
                if (terminal.get().getGrammarType().equals(t)) return terminal;
            }
        }
        return Optional.empty();
    }
}
