package grammarLayer.impl;

import grammarLayer.GrammarParser;
import grammarLayer.dataStruct.ASTLeaf;
import grammarLayer.dataStruct.ASTNode;
import grammarLayer.dataStruct.GrammarType;
import lexLayer.LexicalParser;
import lexLayer.dataStruct.Token;
import lexLayer.impl.LexicalParserImpl;
import utils.LoggerUtil;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.logging.Logger;

public class RecursiveDescentParser implements GrammarParser {
    private final static Logger LOGGER = LoggerUtil.getLogger();
    private final LexicalParser lexicalParser = LexicalParserImpl.getInstance();
    private final ArrayList<Token> tokens = lexicalParser.getAllTokens();
    private Token nowToken = tokens.get(0);
    private ASTNode AST;
    private int nowIndex = 0;

    private void error() {
        LOGGER.severe("Error");
    }

    private void next() {
        if (nowIndex < tokens.size() - 1) {
            nowIndex++;
            nowToken = tokens.get(nowIndex);
        }
    }

    private Optional<Token> preRead(int number) {
        if (nowIndex + number < tokens.size()) {
            return Optional.of(tokens.get(nowIndex + number));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Note that this function can ONLY JUDGE TERMINAL tokens!
     *
     * @param number pre-read how many tokens.
     * @param type   the expected type of pre-reading token.
     * @return true if the token type is compatible with `type`.
     */
    private boolean judgePreReadTerminal(int number, GrammarType type) {
        var token = preRead(number);
        if (token.isEmpty()) return false;
        Optional<GrammarType> grammarType = GrammarType.ofTerminal(token.get().getLexType());
        return grammarType.isPresent() && grammarType.get().equals(type);
    }

    public ASTNode getAST() {
        return this.AST;
    }

    private ASTNode begin(GrammarType type) {
        nowToken = tokens.get(nowIndex);
        return new ASTNode(type);
    }

    private Optional<ASTNode> failed(int initIndex) {
        nowIndex = initIndex;
        nowToken = tokens.get(nowIndex);
        return Optional.empty();
    }

    private <T extends ASTNode> Optional<T> done(T ast) {
        nowToken = tokens.get(nowIndex);
        return Optional.of(ast);
    }

    public void parse() {
        Optional<ASTNode> astNode = this.parseCompUnit();
        astNode.ifPresent(node -> this.AST = node);
    }


    /**
     * CompUnit → {Decl} {FuncDef} MainFuncDef
     *
     * @return Optional<ASTNode> representing the parsed CompUnit
     */
    @Override
    public Optional<ASTNode> parseCompUnit() {
        ASTNode compUnit = new ASTNode(GrammarType.COMP_UNIT);
        Optional<ASTNode> decl, funcDef;
        while ((decl = parseDecl()).isPresent()) {
            compUnit.addChild(decl.get());
        }

        while ((funcDef = parseFuncDef()).isPresent()) {
            compUnit.addChild(funcDef.get());
        }
        parseMainFuncDef().ifPresentOrElse(compUnit::addChild, this::error);
        this.AST = compUnit;
        return done(compUnit);
    }

    /**
     * Decl → ConstDecl | VarDecl
     *
     * @return Optional<ASTNode> representing the parsed Decl
     */
    @Override
    public Optional<ASTNode> parseDecl() {
        int initIndex = nowIndex;
        ASTNode decl = begin(GrammarType.DECL);
        var constDecl = parseConstDecl();//test ok
        if (constDecl.isPresent()) {
            decl.addChild(constDecl.get());
            return done(decl);
        } else {
            failed(initIndex);
        }//test ok

        var varDecl = parseVarDecl();
        if (varDecl.isPresent()) {
            decl.addChild(varDecl.get());
            return done(decl);
        }
        return failed(initIndex);
    }

    /**
     * ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
     *
     * @return Optional<ASTNode> representing the parsed ConstDecl
     */
    @Override
    public Optional<ASTNode> parseConstDecl() {
        int initIndex = nowIndex;
        ASTNode constDecl = begin(GrammarType.CONST_DECL);
        Optional<ASTLeaf> constTk = parseTerminal(GrammarType.CONST);
        if (constTk.isPresent()) {
            constDecl.addChild(constTk.get());
        } else {
            return failed(initIndex);
        }

        Optional<ASTNode> bType = parseBType();
        if (bType.isPresent()) {
            constDecl.addChild(bType.get());
        } else {
            return failed(initIndex);
        }


        Optional<ASTNode> constDef = parseConstDef();
        if (constDef.isPresent()) {
            constDecl.addChild(constDef.get());
        } else {
            return failed(initIndex);
        }

        Optional<ASTLeaf> comma;
        while ((comma = parseTerminal(GrammarType.COMMA)).isPresent()) {
            constDecl.addChild(comma.get());

            constDef = parseConstDef();
            if (constDef.isPresent()) {
                constDecl.addChild(constDef.get());
            } else {
                return failed(initIndex);
            }
        }

        Optional<ASTLeaf> semicolon = parseTerminal(GrammarType.SEMICOLON);
        if (semicolon.isPresent()) {
            constDecl.addChild(semicolon.get());
            return done(constDecl);
        }

        return failed(initIndex);
    }

    /**
     * BType → 'int'
     *
     * @return Optional<ASTNode> representing the parsed BType
     */
    @Override
    public Optional<ASTNode> parseBType() {
        int initIndex = nowIndex;
        ASTNode bType = begin(GrammarType.B_TYPE);
        Optional<ASTLeaf> intTk = parseTerminal(GrammarType.INT);
        if (intTk.isPresent()) {
            bType.addChild(intTk.get());
            return done(bType);
        }

        return failed(initIndex);
    }

    /**
     * ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
     *
     * @return Optional<ASTNode> representing the parsed ConstDef
     */
    @Override
    public Optional<ASTNode> parseConstDef() {
        int initIndex = nowIndex;
        ASTNode constDef = begin(GrammarType.CONST_DEF);

        //Ident
        Optional<ASTLeaf> ident = parseTerminal(GrammarType.IDENTIFIER);
        if (ident.isPresent()) {
            constDef.addChild(ident.get());

        } else {
            return failed(initIndex);
        }

        // '[' ConstExp ']'
        Optional<ASTLeaf> leftBracket;
        while ((leftBracket = parseTerminal(GrammarType.LEFT_BRACKET)).isPresent()) {
            constDef.addChild(leftBracket.get());

            Optional<ASTNode> constExp = parseConstExp();
            if (constExp.isPresent()) {
                constDef.addChild(constExp.get());
            } else {
                return failed(initIndex);
            }
            Optional<ASTLeaf> rightBracket = parseTerminal(GrammarType.RIGHT_BRACKET);
            if (rightBracket.isPresent()) {
                constDef.addChild(rightBracket.get());
            } else {
                return failed(initIndex);
            }
        }

        //'=' ConstInitVal
        Optional<ASTLeaf> assign = parseTerminal(GrammarType.ASSIGN);
        if (assign.isPresent()) {
            constDef.addChild(assign.get());

            Optional<ASTNode> constInitVal = parseConstInitVal();
            if (constInitVal.isPresent()) {
                constDef.addChild(constInitVal.get());
                return done(constDef);
            }
        }

        return failed(initIndex);

    }

    /**
     * ConstInitVal → ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
     *
     * @return Optional<ASTNode> representing the parsed ConstInitVal
     */
    @Override
    public Optional<ASTNode> parseConstInitVal() {
        int initIndex = nowIndex;
        ASTNode constInitVal = begin(GrammarType.CONST_INIT_VAL);

        Optional<ASTNode> constExp = parseConstExp();
        if (constExp.isPresent()) {
            constInitVal.addChild(constExp.get());
            return done(constInitVal);
        } else {
            failed(initIndex);
        }

        // '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
        Optional<ASTLeaf> leftBrace = parseTerminal(GrammarType.LEFT_BRACE);
        if (leftBrace.isPresent()) {
            constInitVal.addChild(leftBrace.get());

            Optional<ASTNode> constInitVal1 = parseConstInitVal();
            if (constInitVal1.isPresent()) {
                constInitVal.addChild(constInitVal1.get());
            } else {
                failed(initIndex);
            }
            Optional<ASTLeaf> comma;
            while ((comma = parseTerminal(GrammarType.COMMA)).isPresent()) {
                constInitVal.addChild(comma.get());

                constInitVal1 = parseConstInitVal();
                if (constInitVal1.isPresent()) {
                    constInitVal.addChild(constInitVal1.get());
                } else {
                    failed(initIndex);
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
        int initIndex = nowIndex;
        var varDecl = begin(GrammarType.VAR_DECL);
        Optional<ASTNode> bType = parseBType();
        if (bType.isPresent()) {
            varDecl.addChild(bType.get());
        } else {
            return failed(initIndex);
        }

        Optional<ASTNode> varDef = parseVarDef();
        if (varDef.isPresent()) {
            varDecl.addChild(varDef.get());
        } else {
            return failed(initIndex);
        }
        Optional<ASTLeaf> comma;
        while ((comma = parseTerminal(GrammarType.COMMA)).isPresent()) {
            varDecl.addChild(comma.get());

            varDef = parseVarDef();
            if (varDef.isPresent()) {
                varDecl.addChild(varDef.get());
            } else {
                return failed(initIndex);
            }
        }

        Optional<ASTLeaf> semicolon = parseTerminal(GrammarType.SEMICOLON);
        if (semicolon.isPresent()) {
            varDecl.addChild(semicolon.get());
            return done(varDecl);
        }

        return failed(initIndex);
    }

    /**
     * VarDef → Ident { '[' ConstExp ']' } ['=' InitVal]
     *
     * @return Optional<ASTNode> representing the parsed VarDef
     */
    @Override
    public Optional<ASTNode> parseVarDef() {
        int initIndex = nowIndex;
        ASTNode varDef = begin(GrammarType.VAR_DEF);
        Optional<ASTNode> constExp, initVal;
        //ident(must)
        Optional<ASTLeaf> ident;
        if ((ident = parseTerminal(GrammarType.IDENTIFIER)).isPresent()) {
            varDef.addChild(ident.get());
        } else {
            return failed(initIndex);
        }
        // { '[' ConstExp ']' } optional
        Optional<ASTLeaf> leftBracket;
        while ((leftBracket = parseTerminal(GrammarType.LEFT_BRACKET)).isPresent()) {
            varDef.addChild(leftBracket.get());

            constExp = parseConstExp();
            if (constExp.isPresent()) {
                varDef.addChild(constExp.get());
            } else {
                return failed(initIndex);
            }
            Optional<ASTLeaf> rightBracket = parseTerminal(GrammarType.RIGHT_BRACKET);
            if (rightBracket.isPresent()) {
                varDef.addChild(rightBracket.get());

            } else {
                return failed(initIndex);
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
                return failed(initIndex);
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
        int initIndex = nowIndex;
        var initVal = begin(GrammarType.INIT_VAL);
        Optional<ASTNode> exp, initVal1;
        Optional<ASTLeaf> leftBrace, comma, rightBrace;
        //Exp
        if ((exp = parseExp()).isPresent()) {
            initVal.addChild(exp.get());
            return done(initVal);
        } else {
            failed(initIndex);
        }

        // '{' [ InitVal { ',' InitVal } ] '}'
        if ((leftBrace = parseTerminal(GrammarType.LEFT_BRACE)).isPresent()) {
            initVal.addChild(leftBrace.get());

            initVal1 = parseInitVal(); //todo has bug: cannot parse {array1[0], 2} (135)
            if (initVal1.isPresent()) {
                initVal.addChild(initVal1.get());
            } else {
                failed(initIndex);
            }

            while ((comma = parseTerminal(GrammarType.COMMA)).isPresent()) {
                initVal.addChild(comma.get());

                initVal1 = parseInitVal();
                if (initVal1.isPresent()) {
                    initVal.addChild(initVal1.get());
                } else {
                    return failed(initIndex);
                }
            }

            if ((rightBrace = parseTerminal(GrammarType.RIGHT_BRACE)).isPresent()) {
                initVal.addChild(rightBrace.get());
                return done(initVal);
            } else return failed(initIndex);
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
        int initIndex = nowIndex;
        ASTNode funcDef = begin(GrammarType.FUNC_DEF);
        Optional<ASTNode> funcType = parseFuncType();
        if (funcType.isPresent()) {
            funcDef.addChild(funcType.get());
        } else {
            return failed(initIndex);
        }

        Optional<ASTLeaf> ident = parseTerminal(GrammarType.IDENTIFIER);
        if (ident.isPresent()) {
            funcDef.addChild(ident.get());

        } else {
            return failed(initIndex);
        }

        Optional<ASTLeaf> leftParen = parseTerminal(GrammarType.LEFT_PAREN);
        if (leftParen.isPresent()) {
            funcDef.addChild(leftParen.get());

        } else {
            return failed(initIndex);
        }

        parseFuncFParams().ifPresent(funcDef::addChild); //optional

        Optional<ASTLeaf> rightParen = parseTerminal(GrammarType.RIGHT_PAREN);
        if (rightParen.isPresent()) {
            funcDef.addChild(rightParen.get());

        } else {
            return failed(initIndex);
        }

        Optional<ASTNode> block = parseBlock();
        if (block.isPresent()) {
            funcDef.addChild(block.get());
        } else {
            return failed(initIndex);
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
        int initIndex = nowIndex;
        ASTNode mainFuncDef = begin(GrammarType.MAIN_FUNC_DEF);
        Optional<ASTLeaf> intTk = parseTerminal(GrammarType.INT);
        if (intTk.isPresent()) {
            mainFuncDef.addChild(intTk.get());
        } else {
            return failed(initIndex);
        }

        Optional<ASTLeaf> mainTk = parseTerminal(GrammarType.MAIN);
        if (mainTk.isPresent()) {
            mainFuncDef.addChild(mainTk.get());

        } else {
            return failed(initIndex);
        }

        Optional<ASTLeaf> leftParen = parseTerminal(GrammarType.LEFT_PAREN);
        if (leftParen.isPresent()) {
            mainFuncDef.addChild(leftParen.get());

        } else {
            return failed(initIndex);
        }

        Optional<ASTLeaf> rightParen = parseTerminal(GrammarType.RIGHT_PAREN);
        if (rightParen.isPresent()) {
            mainFuncDef.addChild(rightParen.get());

        } else {
            return failed(initIndex);
        }
        Optional<ASTNode> block = parseBlock();
        if (block.isPresent()) {
            mainFuncDef.addChild(block.get());
        } else {
            return failed(initIndex);
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
        int initIndex = nowIndex;
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

        return failed(initIndex);
    }

    /**
     * FuncFParams → FuncFParam { ',' FuncFParam }
     *
     * @return Optional<ASTNode> representing the parsed FuncFParams
     */
    @Override
    public Optional<ASTNode> parseFuncFParams() {
        int initIndex = nowIndex;
        ASTNode funcFParams = begin(GrammarType.FUNC_FPARAMS);
        Optional<ASTNode> funcFParam = parseFuncFParam();
        if (funcFParam.isPresent()) {
            funcFParams.addChild(funcFParam.get());
        } else {
            return failed(initIndex);
        }
        Optional<ASTLeaf> comma;
        while ((comma = parseTerminal(GrammarType.COMMA)).isPresent()) {
            funcFParams.addChild(comma.get());

            funcFParam = parseFuncFParam();
            if (funcFParam.isPresent()) {
                funcFParams.addChild(funcFParam.get());
            } else {
                return failed(initIndex);
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
        int initIndex = nowIndex;
        var funcFParam = begin(GrammarType.FUNC_FPARAM);
        Optional<ASTNode> bType = parseBType();
        if (bType.isPresent()) {
            funcFParam.addChild(bType.get());
        } else {
            return failed(initIndex);
        }

        Optional<ASTLeaf> ident = parseTerminal(GrammarType.IDENTIFIER);
        if (ident.isPresent()) {
            funcFParam.addChild(ident.get());

        } else {
            return failed(initIndex);
        }
        Optional<ASTLeaf> leftBracket;
        while ((leftBracket = parseTerminal(GrammarType.LEFT_BRACKET)).isPresent()) {
            funcFParam.addChild(leftBracket.get());
            Optional<ASTLeaf> rightBracket = parseTerminal(GrammarType.RIGHT_BRACKET);
            if (rightBracket.isPresent()) {
                funcFParam.addChild(rightBracket.get());

            } else {
                return failed(initIndex);
            }
            while ((leftBracket = parseTerminal(GrammarType.LEFT_BRACKET)).isPresent()) {
                funcFParam.addChild(leftBracket.get());

                Optional<ASTNode> constExp = parseConstExp();
                if (constExp.isPresent()) {
                    funcFParam.addChild(constExp.get());
                } else {
                    return failed(initIndex);
                }
                rightBracket = parseTerminal(GrammarType.RIGHT_BRACKET);
                if (rightBracket.isPresent()) {
                    funcFParam.addChild(rightBracket.get());

                } else {
                    return failed(initIndex);
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
        int initIndex = nowIndex;
        var Block = begin(GrammarType.BLOCK);
        Optional<ASTLeaf> leftBrace = parseTerminal(GrammarType.LEFT_BRACE);
        if (leftBrace.isPresent()) {
            Block.addChild(leftBrace.get());
        } else {
            return failed(initIndex);
        }

        Optional<ASTNode> blockItem;

        while ((blockItem = parseBlockItem()).isPresent()) {
            Block.addChild(blockItem.get());
        }

        Optional<ASTLeaf> rightBrace = parseTerminal(GrammarType.RIGHT_BRACE);
        if (rightBrace.isPresent()) {
            Block.addChild(rightBrace.get());
        } else {
            return failed(initIndex);
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
        int initIndex = nowIndex;
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

        return failed(initIndex);
    }

    /**
     * Stmt →
     * <p>
     * LVal '=' Exp ';'
     * <p>
     * | LVal '=' 'getint''('')'';'
     * <p>
     * | [Exp] ';'
     * <p>
     * | Block
     * <p>
     * | 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
     * <p>
     * | 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
     * <p>
     * | 'break' ';'
     * <p>
     * | 'continue' ';'
     * <p>
     * | 'return' [Exp] ';'
     * <p>
     * | 'printf''('FormatString{','Exp}')'';'
     *
     * @return Optional<ASTNode> representing the parsed Stmt
     */
    @Override
    public Optional<ASTNode> parseStmt() {
        int initIndex = nowIndex;
        var stmt = begin(GrammarType.STMT);
        Optional<ASTNode> lVal, exp, block;
        Optional<ASTLeaf> semicolon, leftParen, rightParen, getintTk, assign, keyword;

        //need to pre read(Lval and [Exp])
        //the first token is Ident && the second token is LEFT_PAREN
        if (judgePreReadTerminal(0, GrammarType.IDENTIFIER) && judgePreReadTerminal(1, GrammarType.LEFT_PAREN)) {
            // [Exp] ';'
            exp = parseExp();
            if (exp.isPresent()) {
                stmt.addChild(exp.get());

                semicolon = parseTerminal(GrammarType.SEMICOLON);
                if (semicolon.isPresent()) {
                    stmt.addChild(semicolon.get());
                    return done(stmt);
                } else {
                    return failed(initIndex);
                }
            } else {
                semicolon = parseTerminal(GrammarType.SEMICOLON);
                if (semicolon.isPresent()) {
                    stmt.addChild(semicolon.get());
                    return done(stmt);
                }
            }
        }

        // ;
        semicolon = parseTerminal(GrammarType.SEMICOLON);
        if (semicolon.isPresent()) {
            stmt.addChild(semicolon.get());
            return done(stmt);
        }

        //LVal '=' Exp ';' | LVal '=' 'getint''('')'';'
        lVal = parseLVal();
        if (lVal.isPresent()) {
            stmt.addChild(lVal.get());
            assign = parseTerminal(GrammarType.ASSIGN);
            if (assign.isPresent()) {
                stmt.addChild(assign.get());
            } else {
                return failed(initIndex);
            }
            //case 1: exp
            exp = parseExp();
            if (exp.isPresent()) {
                stmt.addChild(exp.get());
                semicolon = parseTerminal(GrammarType.SEMICOLON);
                if (semicolon.isPresent()) {
                    stmt.addChild(semicolon.get());
                    return done(stmt);
                } else {
                    return failed(initIndex);
                }
            }

            //case 2: getint()
            getintTk = parseTerminal(GrammarType.GETINT);
            if (getintTk.isPresent()) {
                stmt.addChild(getintTk.get());
                leftParen = parseTerminal(GrammarType.LEFT_PAREN);
                if (leftParen.isPresent()) {
                    stmt.addChild(leftParen.get());
                } else {
                    return failed(initIndex);
                }
                rightParen = parseTerminal(GrammarType.RIGHT_PAREN);
                if (rightParen.isPresent()) {
                    stmt.addChild(rightParen.get());
                } else {
                    return failed(initIndex);
                }
                semicolon = parseTerminal(GrammarType.SEMICOLON);
                if (semicolon.isPresent()) {
                    stmt.addChild(semicolon.get());
                    return done(stmt);
                } else {
                    return failed(initIndex);
                }
            }

        }


        //Block
        block = parseBlock();
        if (block.isPresent()) {
            stmt.addChild(block.get());
            return done(stmt);
        }

        keyword = parseTerminal(GrammarType.IF, GrammarType.FOR, GrammarType.BREAK, GrammarType.CONTINUE,
                GrammarType.RETURN, GrammarType.PRINTF);
        if (keyword.isEmpty()) {
            return failed(initIndex);
        }

        var grammarType = keyword.get().getGrammarType();
        if (grammarType.equals(GrammarType.IF)) {
            stmt.addChild(keyword.get());
            leftParen = parseTerminal(GrammarType.LEFT_PAREN);
            if (leftParen.isPresent()) {
                stmt.addChild(leftParen.get());
            } else {
                return failed(initIndex);
            }
            var cond = parseCond();
            if (cond.isPresent()) {
                stmt.addChild(cond.get());
            } else {
                return failed(initIndex);
            }
            rightParen = parseTerminal(GrammarType.RIGHT_PAREN);
            if (rightParen.isPresent()) {
                stmt.addChild(rightParen.get());
            } else {
                return failed(initIndex);
            }

            var stmt1 = parseStmt();
            if (stmt1.isPresent()) {
                stmt.addChild(stmt1.get());
            } else {
                return failed(initIndex);
            }
            var elseTk = parseTerminal(GrammarType.ELSE);
            if (elseTk.isPresent()) {
                stmt.addChild(elseTk.get());
                var stmt2 = parseStmt();
                if (stmt2.isPresent()) {
                    stmt.addChild(stmt2.get());
                } else {
                    return failed(initIndex);
                }
            }
            return done(stmt);
        }
        //'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
        else if (grammarType.equals(GrammarType.FOR)) {
            stmt.addChild(keyword.get());
            leftParen = parseTerminal(GrammarType.LEFT_PAREN);
            if (leftParen.isPresent()) {
                stmt.addChild(leftParen.get());
            } else {
                return failed(initIndex);
            }

            Optional<ASTNode> forStmt = parseForStmt();
            forStmt.ifPresent(stmt::addChild);

            var semicolon1 = parseTerminal(GrammarType.SEMICOLON);
            if (semicolon1.isPresent()) {
                stmt.addChild(semicolon1.get());
            } else {
                return failed(initIndex);
            }

            parseCond().ifPresent(stmt::addChild);

            var semicolon2 = parseTerminal(GrammarType.SEMICOLON);
            if (semicolon2.isPresent()) {
                stmt.addChild(semicolon2.get());
            } else {
                return failed(initIndex);
            }

            parseForStmt().ifPresent(stmt::addChild);

            rightParen = parseTerminal(GrammarType.RIGHT_PAREN);
            if (rightParen.isEmpty()) {
                return failed(initIndex);
            }
            stmt.addChild(rightParen.get());
            var stmt3 = parseStmt();
            if (stmt3.isPresent()) {
                stmt.addChild(stmt3.get());
                return done(stmt);
            } else {
                return failed(initIndex);
            }
        }
        //'break' ';'
        else if (grammarType.equals(GrammarType.BREAK)) {
            stmt.addChild(keyword.get());
            semicolon = parseTerminal(GrammarType.SEMICOLON);
            if (semicolon.isPresent()) {
                stmt.addChild(semicolon.get());
                return done(stmt);
            } else {
                return failed(initIndex);
            }
        }
        //'continue' ';'
        else if (grammarType.equals(GrammarType.CONTINUE)) {
            stmt.addChild(keyword.get());
            semicolon = parseTerminal(GrammarType.SEMICOLON);
            if (semicolon.isPresent()) {
                stmt.addChild(semicolon.get());
                return done(stmt);
            } else {
                return failed(initIndex);
            }
        }
        //'return' [Exp] ';'
        else if (grammarType.equals(GrammarType.RETURN)) {
            stmt.addChild(keyword.get());
            parseExp().ifPresent(stmt::addChild);
            semicolon = parseTerminal(GrammarType.SEMICOLON);
            if (semicolon.isPresent()) {
                stmt.addChild(semicolon.get());
                return done(stmt);
            } else {
                return failed(initIndex);
            }
        }
        //'printf''('FormatString{','Exp}')'';'
        else if (grammarType.equals(GrammarType.PRINTF)) {
            stmt.addChild(keyword.get());
            leftParen = parseTerminal(GrammarType.LEFT_PAREN);
            if (leftParen.isPresent()) {
                stmt.addChild(leftParen.get());
            } else {
                return failed(initIndex);
            }

            var formatString = parseTerminal(GrammarType.FORMAT_STRING);
            if (formatString.isPresent()) {
                stmt.addChild(formatString.get());
            } else {
                return failed(initIndex);
            }
            Optional<ASTLeaf> comma;
            while ((comma = parseTerminal(GrammarType.COMMA)).isPresent()) {
                stmt.addChild(comma.get());
                var exp1 = parseExp();
                if (exp1.isPresent()) {
                    stmt.addChild(exp1.get());
                } else {
                    return failed(initIndex);
                }
            }
            Optional<ASTLeaf> rightParen1 = parseTerminal(GrammarType.RIGHT_PAREN);
            if (rightParen1.isPresent()) {
                stmt.addChild(rightParen1.get());
            } else {
                return failed(initIndex);
            }
            Optional<ASTLeaf> semicn = parseTerminal(GrammarType.SEMICOLON);
            if (semicn.isPresent()) {
                stmt.addChild(semicn.get());
            } else {
                return failed(initIndex);
            }
            return done(stmt);
        }
        return failed(initIndex);
    }

    /**
     * ForStmt → LVal '=' Exp
     *
     * @return Optional<ASTNode> representing the parsed ForStmt
     */
    @Override
    public Optional<ASTNode> parseForStmt() {
        int initIndex = nowIndex;
        ASTNode forStmt = begin(GrammarType.FOR_STMT);

        Optional<ASTNode> lVal = parseLVal();
        if (lVal.isEmpty()) {
            return failed(initIndex);
        }
        forStmt.addChild(lVal.get());
        Optional<ASTLeaf> assign = parseTerminal(GrammarType.ASSIGN);
        if (assign.isPresent()) {
            forStmt.addChild(assign.get());
        } else {
            return failed(initIndex);
        }

        Optional<ASTNode> exp = parseExp();
        if (exp.isPresent()) {
            forStmt.addChild(exp.get());
            return done(forStmt);
        }
        return failed(initIndex);
    }

    /**
     * Exp → AddExp
     *
     * @return Optional<ASTNode> representing the parsed Exp
     */
    @Override
    public Optional<ASTNode> parseExp() {
        int initIndex = nowIndex;
        ASTNode exp = begin(GrammarType.EXP);
        var addExp = parseAddExp();
        if (addExp.isPresent()) {
            exp.addChild(addExp.get());
            return done(exp);
        }
        return failed(initIndex);
    }

    /**
     * Cond → LOrExp
     *
     * @return Optional<ASTNode> representing the parsed Cond
     */
    @Override
    public Optional<ASTNode> parseCond() {
        int initIndex = nowIndex;
        ASTNode cond = begin(GrammarType.COND);
        var LorExp = parseLOrExp();
        if (LorExp.isPresent()) {
            cond.addChild(LorExp.get());
            return done(cond);
        }
        return failed(initIndex);
    }

    /**
     * LVal → Ident {'[' Exp ']'}
     *
     * @return Optional<ASTNode> representing the parsedLVal
     */
    @Override
    public Optional<ASTNode> parseLVal() {
        int initIndex = nowIndex;
        ASTNode LVal = begin(GrammarType.LVAL);
        Optional<ASTLeaf> ident = parseTerminal(GrammarType.IDENTIFIER);
        if (ident.isPresent()) {
            LVal.addChild(ident.get());
        } else {
            return failed(initIndex);
        }

        Optional<ASTLeaf> leftParen, rightParen;
        while ((leftParen = parseTerminal(GrammarType.LEFT_BRACKET)).isPresent()) {
            LVal.addChild(leftParen.get());
            Optional<ASTNode> exp = parseExp();
            if (exp.isEmpty()) {
                return failed(initIndex);
            }
            LVal.addChild(exp.get());
            rightParen = parseTerminal(GrammarType.RIGHT_BRACKET);
            if (rightParen.isPresent()) {
                LVal.addChild(rightParen.get());
            } else {
                return failed(initIndex);
            }
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
        int initIndex = nowIndex;
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
        if (leftParen.isEmpty()) return failed(initIndex);
        primaryExp.addChild(leftParen.get());

        var exp = parseExp();
        if (exp.isEmpty()) return failed(initIndex);
        primaryExp.addChild(exp.get());

        var rightParen = parseTerminal(GrammarType.RIGHT_PAREN);
        if (rightParen.isEmpty()) return failed(initIndex);
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
        int initIndex = nowIndex;
        var number = begin(GrammarType.NUMBER);
        var intConst = parseTerminal(GrammarType.INT_CONST);
        if (intConst.isPresent()) {
            number.addChild(intConst.get());
            return done(number);
        }

        return failed(initIndex);
    }

    /**
     * UnaryExp → Ident '(' [FuncRParams] ')' | PrimaryExp | UnaryOp UnaryExp
     *
     * @return Optional<ASTNode> representing the parsed UnaryExp
     */
    @Override
    public Optional<ASTNode> parseUnaryExp() {
        int initIndex = nowIndex;
        var UnaryExp = begin(GrammarType.UNARY_EXP);

        // Ident '(' [FuncRParams] ')'
        if (preRead(1).isPresent() && preRead(1).get().getRawValue().equals("(")) {
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
        }

        //PrimaryExp
        var primaryExp = parsePrimaryExp();
        if (primaryExp.isPresent()) {
            UnaryExp.addChild(primaryExp.get());
            return done(UnaryExp);
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

        return failed(initIndex);
    }

    /**
     * UnaryOp → '+' | '−' | '!'
     *
     * @return Optional<ASTNode> representing the parsed parseUnaryOp
     */
    @Override
    public Optional<ASTNode> parseUnaryOp() {
        int initIndex = nowIndex;
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

        return failed(initIndex);
    }

    /**
     * FuncRParams → Exp { ',' Exp }
     *
     * @return Optional representing the parsed FuncRParams
     */
    @Override
    public Optional<ASTNode> parseFuncRParams() {
        int initIndex = nowIndex;
        var funcRParams = begin(GrammarType.FUNC_RPARAMS);
        var exp = parseExp();
        if (exp.isPresent()) {
            funcRParams.addChild(exp.get());
            Optional<ASTLeaf> comma;
            while ((comma = parseTerminal(GrammarType.COMMA)).isPresent()) {
                funcRParams.addChild(comma.get());
                exp = parseExp();
                if (exp.isPresent()) {
                    funcRParams.addChild(exp.get());
                } else {
                    return failed(initIndex);
                }
            }
            return done(funcRParams);
        }

        return failed(initIndex);
    }

    /**
     * MulExp -> UnaryExp { ('*' | '/' | '%') UnaryExp }
     *
     * @return Optional<ASTNode> representing the parsed MulExp
     */
    @Override
    public Optional<ASTNode> parseMulExp() {
        int initIndex = nowIndex;
        var mulExp = begin(GrammarType.MUL_EXP);

        var unaryExp = parseUnaryExp();
        if (unaryExp.isPresent()) {
            mulExp.addChild(unaryExp.get());
        } else {
            return failed(initIndex);
        }

        Optional<ASTLeaf> operator;
        while ((operator = parseTerminal(GrammarType.MULTIPLY, GrammarType.DIVIDE, GrammarType.MOD)).isPresent()) {
            mulExp.addChild(operator.get());
            unaryExp = parseUnaryExp();
            if (unaryExp.isPresent()) {
                mulExp.addChild(unaryExp.get());
            } else {
                return failed(initIndex);
            }
        }
        return done(mulExp);
    }

    /**
     * AddExp → MulExp {('+'|'-') MulExp}
     *
     * @return Optional<ASTNode> representing the parsed AddExp
     */
    @Override
    public Optional<ASTNode> parseAddExp() {
        int initIndex = nowIndex;
        var addExp = begin(GrammarType.ADD_EXP);

        var mulExp = parseMulExp();
        if (mulExp.isPresent()) {
            addExp.addChild(mulExp.get());
        } else {
            return failed(initIndex);
        }

        Optional<ASTLeaf> operator;
        while ((operator = parseTerminal(GrammarType.PLUS, GrammarType.MINUS)).isPresent()) {
            addExp.addChild(operator.get());
            mulExp = parseMulExp();
            if (mulExp.isPresent()) {
                addExp.addChild(mulExp.get());
            } else {
                return failed(initIndex);
            }
        }

        return done(addExp);
    }

    /**
     * RelExp -> AddExp { ('<' | '>' | '<=' | '>=') AddExp }
     *
     * @return Optional representing the parsed RelExp
     */
    @Override
    public Optional<ASTNode> parseRelExp() {
        int initIndex = nowIndex;
        var relExp = begin(GrammarType.REL_EXP);
        var addExp = parseAddExp();
        if (addExp.isPresent()) {
            relExp.addChild(addExp.get());
        } else {
            return failed(initIndex);
        }

        Optional<ASTLeaf> operator;
        while ((operator = parseTerminal(GrammarType.LESS_THAN, GrammarType.LESS_THAN_EQUAL, GrammarType.GREATER_THAN,
                GrammarType.GREATER_THAN_EQUAL)).isPresent()) {
            relExp.addChild(operator.get());
            addExp = parseAddExp();
            if (addExp.isPresent()) {
                relExp.addChild(addExp.get());
            } else {
                return failed(initIndex);
            }
        }
        return done(relExp);
    }

    /**
     * EqExp -> RelExp { ('==' | '!=') RelExp }
     *
     * @return Optional representing the parsed EqExp
     */
    @Override
    public Optional<ASTNode> parseEqExp() {
        int initIndex = nowIndex;
        var eqExp = begin(GrammarType.EQ_EXP);
        var relExp = parseRelExp();
        if (relExp.isPresent()) {
            eqExp.addChild(relExp.get());
        } else {
            return failed(initIndex);
        }

        Optional<ASTLeaf> operator;
        while ((operator = parseTerminal(GrammarType.EQUAL, GrammarType.NOT_EQUAL)).isPresent()) {
            eqExp.addChild(operator.get());
            relExp = parseRelExp();
            if (relExp.isPresent()) {
                eqExp.addChild(relExp.get());
            } else {
                return failed(initIndex);
            }
        }
        return done(eqExp);
    }

    /**
     * TypeSpec → BType | 'void'
     *
     * @return Optional<ASTNode> representing the parsed TypeSpec
     */
    @Override
    public Optional<ASTNode> parseTypeSpec() {
        int initIndex = nowIndex;
        var typeSpec = begin(GrammarType.TYPE_SPEC);
        var bType = parseBType();
        if (bType.isPresent()) {
            typeSpec.addChild(bType.get());
            return done(typeSpec);
        }

        var voidTk = parseTerminal(GrammarType.VOID);
        if (voidTk.isPresent()) {
            typeSpec.addChild(voidTk.get());
            return done(typeSpec);
        }

        return failed(initIndex);
    }

    /**
     * LAndExp -> EqExp { '&&' EqExp}
     *
     * @return Optional<ASTNode> representing the parsed LAndExp
     */
    @Override
    public Optional<ASTNode> parseLAndExp() {
        int initIndex = nowIndex;
        var lAndExp = begin(GrammarType.LAND_EXP);

        Optional<ASTNode> EqExp;
        EqExp = parseEqExp();
        if (EqExp.isPresent()) lAndExp.addChild(EqExp.get());
        else return failed(initIndex);

        Optional<ASTLeaf> logicalAnd;
        while ((logicalAnd = parseTerminal(GrammarType.LOGICAL_AND)).isPresent()) {
            lAndExp.addChild(logicalAnd.get());
            EqExp = parseEqExp();
            if (EqExp.isEmpty()) return failed(initIndex);
            lAndExp.addChild(EqExp.get());
        }
        return done(lAndExp);
    }

    /**
     * LOrExp -> LAndExp { '||' LAndExp }
     *
     * @return Optional<ASTNode> representing the parsed LOrExp
     */
    @Override
    public Optional<ASTNode> parseLOrExp() {
        int initIndex = nowIndex;
        var LOrExp = begin(GrammarType.LOR_EXP);

        Optional<ASTNode> lAndExp;
        lAndExp = parseLAndExp();
        if (lAndExp.isPresent()) {
            LOrExp.addChild(lAndExp.get());
        } else {
            return failed(initIndex);
        }

        Optional<ASTLeaf> logicalOr;
        while ((logicalOr = parseTerminal(GrammarType.LOGICAL_OR)).isPresent()) {
            LOrExp.addChild(logicalOr.get());
            lAndExp = parseLAndExp();
            if (lAndExp.isEmpty()) return failed(initIndex);
            LOrExp.addChild(lAndExp.get());
        }
        return done(LOrExp);
    }

    /**
     * ConstExp → AddExp
     *
     * @return Optional<ASTNode> representing the parsed ConstExp
     */
    @Override
    public Optional<ASTNode> parseConstExp() {
        int initIndex = nowIndex;
        var constExp = begin(GrammarType.CONST_EXP);
        var addExp = parseAddExp();
        if (addExp.isPresent()) {
            constExp.addChild(addExp.get());
            return done(constExp);
        }

        return failed(initIndex);
    }

    /**
     * Parse the terminals. Include `keyword, punctuation, operator, string, number`.
     *
     * @return Optional ASTLeaf that been parsed.
     */
    @Override
    public Optional<ASTLeaf> parseTerminal(GrammarType... type) {
        ASTLeaf astLeaf;
        try {
            astLeaf = new ASTLeaf(nowToken);
        } catch (NoSuchElementException e) {
            return Optional.empty();
        }

        for (var t : type) {
            if (astLeaf.getGrammarType().equals(t)) {
                next();
                return done(astLeaf);
            }
        }

        return Optional.empty();
    }
}
