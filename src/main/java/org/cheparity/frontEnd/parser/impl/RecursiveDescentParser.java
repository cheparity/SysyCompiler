package frontEnd.parser.impl;

import exception.*;
import frontEnd.lexer.dataStruct.Token;
import frontEnd.parser.SysYParser;
import frontEnd.parser.dataStruct.ASTLeaf;
import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import frontEnd.parser.dataStruct.utils.ParserUtil;
import middleEnd.symbols.*;
import utils.LoggerUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.logging.Logger;

public class RecursiveDescentParser implements SysYParser {
    private final static Logger LOGGER = LoggerUtil.getLogger();
    private final static RecursiveDescentParser PARSER_INSTANCE = new RecursiveDescentParser();
    private List<Token> tokens;
    private Token nowToken;
    private ASTNode AST;
    private int nowIndex = 0;
    private SymbolTable nowSymbolTable; //it is the BLOCK that contains the symbol table

    private RecursiveDescentParser() {
    }

    public static RecursiveDescentParser getInstance() {
        return PARSER_INSTANCE;
    }

    @Override
    public SysYParser setTokens(List<Token> tokens) {
        if (tokens.isEmpty()) {
            throw new NoSuchElementException("No tokens to parse!");
        }
        this.tokens = tokens;
        this.nowToken = tokens.get(0);
        return this;
    }

    private void error(ASTNode node, GrammarError e) {
        //To get which line of `RecursiveDescentParser` throws the error.
        StackTraceElement ste = new Exception().getStackTrace()[1];
        String methodName = ste.getMethodName();
        int lineNumber = ste.getLineNumber();
        LOGGER.severe(methodName + " throws " + e.getMessage() + " at line " + lineNumber);
        node.addGrammarError(e);
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

    @Override
    public ASTNode getAST() {
        if (this.AST == null) {
            this.parse();
        }
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

    @Override
    public void parse() {
        if (this.tokens.isEmpty()) {
            throw new NoSuchElementException("No tokens to parse! Please call [setTokens]");
        }
        Optional<ASTNode> astNode = this.parseCompUnit();
        astNode.ifPresent(node -> this.AST = node);
    }


    /**
     * CompUnit -> {Decl} {FuncDef} MainFuncDef
     *
     * @return Optional<ASTNode> representing the parsed CompUnit
     */
    private Optional<ASTNode> parseCompUnit() {
        ASTNode compUnit = new ASTNode(GrammarType.COMP_UNIT);
        Optional<ASTNode> decl, funcDef;
        this.nowSymbolTable = new SymbolTable(null, compUnit); //Global symbol table
        SymbolTable.setGlobal(this.nowSymbolTable);
        while ((decl = parseDecl()).isPresent()) {
            compUnit.addChild(decl.get());
        }

        while ((funcDef = parseFuncDef()).isPresent()) {
            compUnit.addChild(funcDef.get());
        }
        parseMainFuncDef().ifPresent(compUnit::addChild);
        this.AST = compUnit;
        return done(compUnit);
    }

    /**
     * Decl -> ConstDecl | VarDecl
     *
     * @return Optional<ASTNode> representing the parsed Decl
     */
    private Optional<ASTNode> parseDecl() {
        int initIndex = nowIndex;
        ASTNode decl = begin(GrammarType.DECL);
        var constDecl = parseConstDecl();
        if (constDecl.isPresent()) {
            decl.addChild(constDecl.get());
            return done(decl);
        } else {
            failed(initIndex);
        }

        var varDecl = parseVarDecl();
        if (varDecl.isPresent()) {
            decl.addChild(varDecl.get());
            return done(decl);
        }
        return failed(initIndex);
    }

    /**
     * ConstDecl -> 'const' BType ConstDef { ',' ConstDef } ';'
     *
     * @return Optional<ASTNode> representing the parsed ConstDecl
     */
    private Optional<ASTNode> parseConstDecl() {
        int initIndex = nowIndex;
        ASTNode constDecl = begin(GrammarType.CONST_DECL);
        Optional<ASTLeaf> constTk = parseTerminal(GrammarType.CONST);
        var constDefList = new ArrayList<ASTNode>();
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
            constDefList.add(constDef.get());
        } else {
            return failed(initIndex);
        }

        Optional<ASTLeaf> comma;
        while ((comma = parseTerminal(GrammarType.COMMA)).isPresent()) {
            constDecl.addChild(comma.get());
            constDef = parseConstDef();
            if (constDef.isPresent()) {
                constDecl.addChild(constDef.get());
                constDefList.add(constDef.get());
            } else {
                return failed(initIndex);
            }
        }

        Optional<ASTLeaf> semicolon = parseTerminal(GrammarType.SEMICOLON);
        semicolon.ifPresentOrElse(constDecl::addChild, () -> error(constDecl, new SemicolonMissedError(constDecl.lastToken())));
        for (ASTNode node : constDefList) {
            // ConstDef -> Ident { '[' ConstExp ']' } '=' ConstInitVal
            ASTLeaf ident = (ASTLeaf) node.getChildren().get(0);
            int dim = ParserUtil.getDim4Def(node);
            var symbol = new ConstSymbol(this.nowSymbolTable, ident.getToken(), dim);
            this.nowSymbolTable.addSymbol(symbol, constDecl.getErrorHandler());
        }

        return done(constDecl);
    }

    /**
     * BType -> 'int'
     *
     * @return Optional<ASTNode> representing the parsed BType
     */
    private Optional<ASTNode> parseBType() {
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
     * ConstDef -> Ident { '[' ConstExp ']' } '=' ConstInitVal
     *
     * @return Optional<ASTNode> representing the parsed ConstDef
     */
    private Optional<ASTNode> parseConstDef() {
        int initIndex = nowIndex;
        ASTNode constDef = begin(GrammarType.CONST_DEF);

        //Ident
        Optional<ASTLeaf> ident = parseTerminal(GrammarType.IDENT);
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
            parseTerminal(GrammarType.RIGHT_BRACKET).ifPresentOrElse(constDef::addChild, () -> error(constDef, new RBracketMissedError(constDef.lastToken())));
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
     * ConstInitVal -> ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
     *
     * @return Optional<ASTNode> representing the parsed ConstInitVal
     */
    private Optional<ASTNode> parseConstInitVal() {
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
            rightBrace.ifPresent(constInitVal::addChild);
        }

        return done(constInitVal);
    }

    /**
     * VarDecl -> BType VarDef { ',' VarDef } ';'
     *
     * @return Optional<ASTNode> representing the parsed VarDecl
     */
    private Optional<ASTNode> parseVarDecl() {
        int initIndex = nowIndex;
        var varDecl = begin(GrammarType.VAR_DECL);
        Optional<ASTNode> bType = parseBType();
        var varDefList = new ArrayList<ASTNode>();
        if (bType.isPresent()) {
            varDecl.addChild(bType.get());
        } else {
            return failed(initIndex);
        }

        Optional<ASTNode> varDef = parseVarDef();
        if (varDef.isPresent()) {
            varDecl.addChild(varDef.get());
            varDefList.add(varDef.get());
        } else {
            return failed(initIndex);
        }
        Optional<ASTLeaf> comma;
        while ((comma = parseTerminal(GrammarType.COMMA)).isPresent()) {
            varDecl.addChild(comma.get());

            varDef = parseVarDef();
            if (varDef.isPresent()) {
                varDecl.addChild(varDef.get());
                varDefList.add(varDef.get());
            } else {
                return failed(initIndex);
            }
        }

        Optional<ASTLeaf> semicolon = parseTerminal(GrammarType.SEMICOLON);
        if (semicolon.isEmpty()) {
            error(varDecl, new SemicolonMissedError(varDecl.lastToken()));
            return failed(initIndex);
        }
        varDecl.addChild(semicolon.get());
        for (ASTNode node : varDefList) {
            // VarDef -> Ident { '[' ConstExp ']' } ['=' InitVal]
            ASTLeaf ident = (ASTLeaf) node.getChildren().get(0);
            int dim = ParserUtil.getDim4Def(node);
            var symbol = new VarSymbol(this.nowSymbolTable, ident.getToken(), dim);
            this.nowSymbolTable.addSymbol(symbol, varDecl.getErrorHandler());
        }
        return done(varDecl);
    }

    /**
     * VarDef -> Ident { '[' ConstExp ']' } ['=' InitVal]
     *
     * @return Optional<ASTNode> representing the parsed VarDef
     */
    private Optional<ASTNode> parseVarDef() {
        int initIndex = nowIndex;
        ASTNode varDef = begin(GrammarType.VAR_DEF);
        Optional<ASTNode> constExp, initVal;
        //ident(must)
        Optional<ASTLeaf> ident;
        if ((ident = parseTerminal(GrammarType.IDENT)).isPresent()) {
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
            parseTerminal(GrammarType.RIGHT_BRACKET).ifPresentOrElse(varDef::addChild, () -> error(varDef, new RBracketMissedError(varDef.lastToken())));
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
     * InitVal -> Exp | '{' [ InitVal { ',' InitVal } ] '}'
     *
     * @return Optional<ASTNode> representing the parsed InitVal
     */
    private Optional<ASTNode> parseInitVal() {
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

            initVal1 = parseInitVal();
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
            }
        }

        return done(initVal);
    }

    /**
     * FuncDef -> FuncType Ident '(' [FuncFParams] ')' Block
     *
     * @return Optional<ASTNode> representing the parsed FuncDef
     */
    private Optional<ASTNode> parseFuncDef() {
        int initIndex = nowIndex;
        ASTNode funcDef = begin(GrammarType.FUNC_DEF);
        FuncSymbol symbol;
        List<VarSymbol> params = new ArrayList<>(); //（important）存放函数参数的数组

        Optional<ASTNode> funcType = parseFuncType();
        if (funcType.isPresent()) {
            funcDef.addChild(funcType.get());
        } else {
            return failed(initIndex);
        }
        GrammarType t = funcType.get().getChildren().get(0).getGrammarType();
        FuncType type = (t.equals(GrammarType.VOID) ? FuncType.VOID : FuncType.INT);

        Optional<ASTLeaf> ident = parseTerminal(GrammarType.IDENT);
        if (ident.isPresent()) {
            funcDef.addChild(ident.get());
            symbol = new FuncSymbol(this.nowSymbolTable, ident.get().getToken(), type);
            nowSymbolTable.addSymbol(symbol, funcDef.getErrorHandler());
        } else {
            return failed(initIndex);
        }

        Optional<ASTLeaf> leftParen = parseTerminal(GrammarType.LEFT_PAREN);
        if (leftParen.isPresent()) {
            funcDef.addChild(leftParen.get());
        } else {
            return failed(initIndex);
        }

        Optional<ASTNode> funcFParams = parseFuncFParams(params);
        funcFParams.ifPresent(funcDef::addChild);
        symbol.setFParams(params);

        Optional<ASTLeaf> rightParen = parseTerminal(GrammarType.RIGHT_PAREN);
        rightParen.ifPresentOrElse(funcDef::addChild, () -> error(funcDef, new RParenMissedError(funcDef.lastToken())));
        //check return
        Optional<ASTNode> block = parseBlock(params.toArray(new VarSymbol[0]));
        if (block.isPresent()) {
            funcDef.addChild(block.get());
        } else {
            return failed(initIndex);
        }
        Optional<ASTNode> ret = block.get().deepDownFind(GrammarType.RETURN, 3);
        if (type == FuncType.INT && ret.isEmpty()) {
            error(funcDef, new RetStmtMissedError(block.get().lastToken()));
        } else if (type == FuncType.VOID && ret.isPresent() && ret.get().getFather().getChildren().size() > 2) {
            error(funcDef, new RedundantRetStmtError(((ASTLeaf) ret.get()).getToken()));
        }
        symbol.setDim(type);
        return done(funcDef);
    }

    /**
     * MainFuncDef -> 'int' 'main' '(' ')' Block
     *
     * @return Optional<ASTNode> representing the parsed MainFuncDef
     */
    private Optional<ASTNode> parseMainFuncDef() {
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

        parseTerminal(GrammarType.RIGHT_PAREN).ifPresentOrElse(mainFuncDef::addChild, () -> error(mainFuncDef,
                new RParenMissedError(mainFuncDef.lastToken())));
        Optional<ASTNode> block = parseBlock();
        if (block.isPresent()) {
            mainFuncDef.addChild(block.get());
        } else {
            return failed(initIndex);
        }

        Optional<ASTNode> retNode = block.get().deepDownFind(GrammarType.RETURN, 3);
        if (retNode.isEmpty()) {
            error(mainFuncDef, new RetStmtMissedError(block.get().lastToken()));
        }
        //全局符号表中注册main函数
        var globalSymbolTable = SymbolTable.getGlobal();
        globalSymbolTable.addSymbol(new FuncSymbol(globalSymbolTable, mainTk.get().getToken(), FuncType.INT),
                mainFuncDef.getErrorHandler());
        return done(mainFuncDef);
    }

    /**
     * FuncType -> 'void' | 'int'
     *
     * @return Optional<ASTNode> representing the parsed FuncType
     */
    private Optional<ASTNode> parseFuncType() {
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
     * FuncFParams -> FuncFParam { ',' FuncFParam }
     *
     * @return Optional<ASTNode> representing the parsed FuncFParams
     */
    private Optional<ASTNode> parseFuncFParams(List<VarSymbol> params) {
        int initIndex = nowIndex;
        ASTNode funcFParams = begin(GrammarType.FUNC_FPARAMS);

        Optional<ASTNode> funcFParam = parseFuncFParam(params);
        if (funcFParam.isPresent()) {
            funcFParams.addChild(funcFParam.get());
        } else {
            return failed(initIndex);
        }
        Optional<ASTLeaf> comma;
        while ((comma = parseTerminal(GrammarType.COMMA)).isPresent()) {
            funcFParams.addChild(comma.get());

            funcFParam = parseFuncFParam(params);
            if (funcFParam.isPresent()) {
                funcFParams.addChild(funcFParam.get());
            } else {
                return failed(initIndex);
            }
        }

        return done(funcFParams);
    }

    /**
     * FuncFParam -> BType Ident ['[' ']' { '[' ConstExp ']' }]
     *
     * @return Optional<ASTNode> representing the parsed FuncFParam
     */
    private Optional<ASTNode> parseFuncFParam(List<VarSymbol> funcParams) {
        int initIndex = nowIndex;
        var funcFParam = begin(GrammarType.FUNC_FPARAM);
        Optional<ASTNode> bType = parseBType();
        if (bType.isPresent()) {
            funcFParam.addChild(bType.get());
        } else {
            return failed(initIndex);
        }

        Optional<ASTLeaf> ident = parseTerminal(GrammarType.IDENT);
        if (ident.isPresent()) {
            funcFParam.addChild(ident.get());

        } else {
            return failed(initIndex);
        }
        Optional<ASTLeaf> leftBracket;
        while ((leftBracket = parseTerminal(GrammarType.LEFT_BRACKET)).isPresent()) {
            funcFParam.addChild(leftBracket.get());
            parseTerminal(GrammarType.RIGHT_BRACKET).ifPresentOrElse(funcFParam::addChild, () -> error(funcFParam,
                    new RBracketMissedError(funcFParam.lastToken())));
            while ((leftBracket = parseTerminal(GrammarType.LEFT_BRACKET)).isPresent()) {
                funcFParam.addChild(leftBracket.get());

                Optional<ASTNode> constExp = parseConstExp();
                if (constExp.isPresent()) {
                    funcFParam.addChild(constExp.get());
                } else {
                    return failed(initIndex);
                }
                parseTerminal(GrammarType.RIGHT_BRACKET).ifPresentOrElse(funcFParam::addChild, () -> error(funcFParam,
                        new RBracketMissedError(funcFParam.lastToken())));
            }
        }
        var dim = ParserUtil.getDim4Def(funcFParam);
        VarSymbol symbol = new VarSymbol(this.nowSymbolTable, ident.get().getToken(), dim);
        funcParams.add(symbol);
//        this.nowSymbolTable.addSymbol(symbol, funcFParam.getErrorHandler()); 修改：把symbol传递出去，在block里添加

        return done(funcFParam);
    }

    /**
     * Block -> '{' { BlockItem } '}'
     *
     * @return Optional<ASTNode> representing the parsed Block
     */
    private Optional<ASTNode> parseBlock(VarSymbol... funcParams) {
        int initIndex = nowIndex;
        var block = begin(GrammarType.BLOCK);
        this.nowSymbolTable = new SymbolTable(this.nowSymbolTable, block);
        //修改：把函数参数添加到块级符号表中
        for (Symbol symbol : funcParams) {
            this.nowSymbolTable.addSymbol(symbol, block.getErrorHandler());
        }
        Optional<ASTLeaf> leftBrace = parseTerminal(GrammarType.LEFT_BRACE);
        if (leftBrace.isPresent()) {
            block.addChild(leftBrace.get());
        } else {
            this.nowSymbolTable = nowSymbolTable.getOuter();//释放符号表
            return failed(initIndex);
        }

        Optional<ASTNode> blockItem;

        while ((blockItem = parseBlockItem()).isPresent()) {
            block.addChild(blockItem.get());
        }

        Optional<ASTLeaf> rightBrace = parseTerminal(GrammarType.RIGHT_BRACE);
        if (rightBrace.isPresent()) {
            block.addChild(rightBrace.get());
        } else {
            this.nowSymbolTable = nowSymbolTable.getOuter();//释放符号表
            return failed(initIndex);
        }
        this.nowSymbolTable = this.nowSymbolTable.getOuter();
        return done(block);
    }

    /**
     * BlockItem -> Decl | Stmt
     *
     * @return Optional<ASTNode> representing the parsed BlockItem
     */
    private Optional<ASTNode> parseBlockItem() {
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

    private boolean preRead4ParseStmt2JudgeLVal() {
        int initIndex = nowIndex;
        if (parseLVal().isEmpty()) {
            failed(initIndex);
            return false;
        }
        if (parseTerminal(GrammarType.ASSIGN).isEmpty()) {
            failed(initIndex);
            return false;
        }
        failed(initIndex);
        return true;
    }

    /**
     * Stmt ->
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
    private Optional<ASTNode> parseStmt() {
        int initIndex = nowIndex;
        var stmt = begin(GrammarType.STMT);
        Optional<ASTNode> lVal, exp, block;
        Optional<ASTLeaf> semicolon, leftParen, getintTk, assign, keyword;

        if (preRead4ParseStmt2JudgeLVal()) {
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
                //check if const
                Optional<ASTNode> ident = lVal.get().deepDownFind(GrammarType.IDENT, 1);
                assert ident.isPresent();
                Token token = ((ASTLeaf) ident.get()).getToken();
                Optional<Symbol> s = nowSymbolTable.getSymbol(token.getRawValue());
                if (s.isPresent() && s.get().getType().equals(SymbolType.CONST)) {
                    error(stmt, new ConstChangedError(token));
                }

                //case 1: exp
                exp = parseExp();
                if (exp.isPresent()) {
                    stmt.addChild(exp.get());
                    parseTerminal(GrammarType.SEMICOLON).ifPresentOrElse(stmt::addChild, () -> error(stmt, new SemicolonMissedError(stmt.lastToken())));
                    return done(stmt);
                }

                //case 2: getint()
                getintTk = parseTerminal(GrammarType.GETINT);
                if (getintTk.isPresent()) {
                    stmt.addChild(getintTk.get());
                    parseTerminal(GrammarType.LEFT_PAREN).ifPresent(stmt::addChild);
                    parseTerminal(GrammarType.RIGHT_PAREN).ifPresentOrElse(stmt::addChild, () -> error(stmt,
                            new RParenMissedError(stmt.lastToken())));
                    parseTerminal(GrammarType.SEMICOLON).ifPresentOrElse(stmt::addChild, () -> error(stmt, new SemicolonMissedError(stmt.lastToken())));
                    return done(stmt);
                }
            }
        }

        //need to pre read(Lval and [Exp])
        //the first token is Ident && the second token is LEFT_PAREN
//        if (judgePreReadTerminal(0, GrammarType.IDENTIFIER) && judgePreReadTerminal(1, GrammarType.LEFT_PAREN)) {
        // [Exp] ';'
        exp = parseExp();
        if (exp.isPresent()) {
            stmt.addChild(exp.get());
            parseTerminal(GrammarType.SEMICOLON).ifPresentOrElse(stmt::addChild, () -> error(stmt, new SemicolonMissedError(stmt.lastToken())));
            return done(stmt);
        }

        //Block
        block = parseBlock();
        if (block.isPresent()) {
            stmt.addChild(block.get());
            return done(stmt);
        }

        //keyword stmt
        keyword = parseTerminal(GrammarType.IF, GrammarType.FOR, GrammarType.BREAK, GrammarType.CONTINUE, GrammarType.RETURN, GrammarType.PRINTF);
        if (keyword.isPresent()) {
            var grammarType = keyword.get().getGrammarType();
            // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
            if (grammarType.equals(GrammarType.IF)) {
                stmt.addChild(keyword.get());
                parseTerminal(GrammarType.LEFT_PAREN).ifPresent(stmt::addChild);
                parseCond().ifPresent(stmt::addChild);
                parseTerminal(GrammarType.RIGHT_PAREN).ifPresentOrElse(stmt::addChild, () -> error(stmt,
                        new RParenMissedError(stmt.lastToken())));
                parseStmt().ifPresent(stmt::addChild);
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
                parseTerminal(GrammarType.LEFT_PAREN).ifPresent(stmt::addChild);
                parseForStmt().ifPresent(stmt::addChild);
                parseTerminal(GrammarType.SEMICOLON).ifPresentOrElse(stmt::addChild, () -> error(stmt, new SemicolonMissedError(stmt.lastToken())));
                parseCond().ifPresent(stmt::addChild);
                parseTerminal(GrammarType.SEMICOLON).ifPresentOrElse(stmt::addChild, () -> error(stmt, new SemicolonMissedError(stmt.lastToken())));
                parseForStmt().ifPresent(stmt::addChild);
                parseTerminal(GrammarType.RIGHT_PAREN).ifPresentOrElse(stmt::addChild, () -> error(stmt,
                        new RParenMissedError(stmt.lastToken())));
                parseStmt().ifPresent(stmt::addChild);
                return done(stmt);
            }
            //'break' ';' | 'continue' ';'
            else if (grammarType.equals(GrammarType.BREAK) || grammarType.equals(GrammarType.CONTINUE)) {
                stmt.addChild(keyword.get());
                semicolon = parseTerminal(GrammarType.SEMICOLON);
                Optional<ASTNode> forStmt = keyword.get().deepUpFind(GrammarType.FOR_STMT);
                if (forStmt.isEmpty()) error(stmt, new NotLoopStmtError(keyword.get().getToken()));
                if (semicolon.isPresent()) {
                    stmt.addChild(semicolon.get());
                    return done(stmt);
                } else {
                    error(stmt, new SemicolonMissedError(stmt.lastToken()));
                }
            }
            //'return' [Exp] ';'
            else if (grammarType.equals(GrammarType.RETURN)) {
                stmt.addChild(keyword.get());
                parseExp().ifPresent(stmt::addChild);
                semicolon = parseTerminal(GrammarType.SEMICOLON);
                semicolon.ifPresentOrElse(stmt::addChild, () -> error(stmt, new SemicolonMissedError(stmt.lastToken())));
                return done(stmt);
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
                //check if formatString only contains '%d'
                var rawValue = formatString.get().getToken().getRawValue().toCharArray();
                int fmtCNum = 0;
                for (int i = 1; i < rawValue.length - 1; i++) {
                    int ascii = rawValue[i];
                    if (rawValue[i] == '%') {
                        if (i + 1 < rawValue.length && rawValue[i + 1] == 'd')
                            fmtCNum++;
                        else error(stmt, new InvalidFormatStringError(formatString.get().getToken()));
                    } else if (rawValue[i] == '\\' && (i + 1 >= rawValue.length || rawValue[i + 1] != 'n')) {
                        error(stmt, new InvalidFormatStringError(formatString.get().getToken()));
                    } else if (ascii < 32 || (ascii > 33 && ascii < 40) || ascii > 126) {
                        //ascii must between 32, 33, 40-126
                        error(stmt, new InvalidFormatStringError(formatString.get().getToken()));
                    }

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
                    fmtCNum--;
                }
                if (fmtCNum != 0) {
                    error(stmt, new PrintfUnmatchedError(keyword.get().getToken()));
                }
                parseTerminal(GrammarType.RIGHT_PAREN).ifPresentOrElse(stmt::addChild, () -> error(stmt,
                        new RParenMissedError(stmt.lastToken())));
                parseTerminal(GrammarType.SEMICOLON).ifPresentOrElse(stmt::addChild, () -> error(stmt, new SemicolonMissedError(stmt.lastToken())));
                return done(stmt);
            }
        } else failed(initIndex);

        // ; (finally)
        semicolon = parseTerminal(GrammarType.SEMICOLON);
        if (semicolon.isPresent()) {
            stmt.addChild(semicolon.get());
            return done(stmt);
        }
//        error(stmt, new SemicolonMissedException(stmt.lastToken()));
        return failed(initIndex);
    }

    /**
     * ForStmt -> LVal '=' Exp
     *
     * @return Optional<ASTNode> representing the parsed ForStmt
     */
    private Optional<ASTNode> parseForStmt() {
        int initIndex = nowIndex;
        ASTNode forStmt = begin(GrammarType.FOR_STMT);

        Optional<ASTNode> lVal = parseLVal();
        if (lVal.isEmpty()) {
            return failed(initIndex);
        }
        forStmt.addChild(lVal.get());
        //check const
        Optional<ASTNode> ident = lVal.get().deepDownFind(GrammarType.IDENT, 1);
        assert ident.isPresent();
        Optional<Symbol> s = nowSymbolTable.getSymbol(((ASTLeaf) ident.get()).getToken().getRawValue());
        if (s.isPresent() && s.get().getType().equals(SymbolType.CONST)) {
            error(forStmt, new ConstChangedError(((ASTLeaf) ident.get()).getToken()));
        } else if (s.isEmpty()) {
            error(forStmt, new UndefinedIdentError(((ASTLeaf) ident.get()).getToken()));
        }

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
     * Exp -> AddExp
     *
     * @return Optional<ASTNode> representing the parsed Exp
     */
    private Optional<ASTNode> parseExp() {
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
     * Cond -> LOrExp
     *
     * @return Optional<ASTNode> representing the parsed Cond
     */
    private Optional<ASTNode> parseCond() {
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
     * LVal -> Ident {'[' Exp ']'}
     *
     * @return Optional<ASTNode> representing the parsedLVal
     */
    private Optional<ASTNode> parseLVal() {
        int initIndex = nowIndex;
        ASTNode LVal = begin(GrammarType.LVAL);
        Optional<ASTLeaf> ident = parseTerminal(GrammarType.IDENT);
        if (ident.isPresent()) {
            LVal.addChild(ident.get());
            Optional<Symbol> s = nowSymbolTable.getSymbol(ident.get().getToken().getRawValue());
            if (s.isEmpty()) {
                var e = new UndefinedIdentError(ident.get().getToken());
                error(LVal, e);
            }
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
     * PrimaryExp ->  LVal | Number | '(' Exp ')'
     *
     * @return Optional<ASTNode> representing the parsed PrimaryExp
     */
    private Optional<ASTNode> parsePrimaryExp() {
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
     * Number -> IntConst
     *
     * @return Optional<ASTNode> representing the parsed Number
     */
    private Optional<ASTNode> parseNumber() {
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
     * UnaryExp -> Ident '(' [FuncRParams] ')' | PrimaryExp | UnaryOp UnaryExp
     *
     * @return Optional<ASTNode> representing the parsed UnaryExp
     */
    private Optional<ASTNode> parseUnaryExp() {
        int initIndex = nowIndex;
        var UnaryExp = begin(GrammarType.UNARY_EXP);

        // Ident '(' [FuncRParams] ')'
        if (preRead(1).isPresent() && preRead(1).get().getRawValue().equals("(")) {
            var ident = parseTerminal(GrammarType.IDENT);
            if (ident.isPresent()) {
                UnaryExp.addChild(ident.get());
                FuncSymbol funcSym = null;
                Optional<FuncSymbol> funcSymOpt = nowSymbolTable.getFuncSymbol(ident.get().getToken().getRawValue());
                //check ident symbol is present.
                if (funcSymOpt.isEmpty()) {
                    error(UnaryExp, new UndefinedIdentError(ident.get().getToken()));
                } else {
                    // if present, check if params count is matched
                    funcSym = funcSymOpt.get();
                }

                var leftParen = parseTerminal(GrammarType.LEFT_PAREN);
                if (leftParen.isPresent()) {
                    UnaryExp.addChild(leftParen.get());
                    parseFuncRParams(funcSym, ident.get().getToken()).ifPresent(UnaryExp::addChild);
                    parseTerminal(GrammarType.RIGHT_PAREN).ifPresentOrElse(UnaryExp::addChild, () -> error(UnaryExp, new RParenMissedError(UnaryExp.lastToken())));
                    return done(UnaryExp);
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
     * UnaryOp -> '+' | '−' | '!'
     *
     * @return Optional<ASTNode> representing the parsed parseUnaryOp
     */
    private Optional<ASTNode> parseUnaryOp() {
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
     * FuncRParams -> Exp { ',' Exp }
     *
     * @return Optional representing the parsed FuncRParams
     */
    private Optional<ASTNode> parseFuncRParams(FuncSymbol funcSymbolOpt, Token funcCallTk) {
        int initIndex = nowIndex;
        var funcRParams = begin(GrammarType.FUNC_RPARAMS);

        List<ASTNode> nodeList = new ArrayList<>();
        var exp = parseExp();
        if (exp.isEmpty()) return failed(initIndex);
        nodeList.add(exp.get());
        funcRParams.addChild(exp.get());
        Optional<ASTLeaf> comma;
        while ((comma = parseTerminal(GrammarType.COMMA)).isPresent()) {
            funcRParams.addChild(comma.get());
            exp = parseExp();
            if (exp.isPresent()) {
                funcRParams.addChild(exp.get());
                nodeList.add(exp.get());
            } else {
                return failed(initIndex);
            }
        }
        if (funcSymbolOpt == null) return done(funcRParams);
        int expectParamsNum = funcSymbolOpt.getParamCount();
        List<VarSymbol> expectParams = funcSymbolOpt.getParams();
        int actualParamsNum = nodeList.size();
        if (expectParamsNum != actualParamsNum) {
            error(funcRParams, new FuncParamCntNotMatchedError(expectParamsNum, actualParamsNum, funcCallTk));
        } else {
            for (int i = 0; i < expectParamsNum; i++) {
                var e = nodeList.get(i);
                var actualDim = ParserUtil.getExpDim(e, nowSymbolTable);
                var expectDim = expectParams.get(i).getDim();
                if (actualDim == expectDim) break;
                error(funcRParams, new FuncParamTypeNotMatchedError("dim " + expectDim, "dim " + actualDim, funcCallTk));
            }
        }
        return done(funcRParams);
    }

    /**
     * MulExp -> UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
     * <p>
     * MulExp -> UnaryExp { ('*' | '/' | '%') UnaryExp }
     *
     * @return Optional<ASTNode> representing the parsed MulExp
     */
    private Optional<ASTNode> parseMulExp() {
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
            ASTNode mul = new ASTNode(GrammarType.MUL_EXP);
            mul.addChild(unaryExp.get());
            mulExp.replaceLastChild(mul);

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
     * AddExp -> MulExp | AddExp ('+' | '−') MulExp
     * <p>
     * AddExp -> MulExp {('+'|'-') MulExp}
     *
     * @return Optional<ASTNode> representing the parsed AddExp
     */
    private Optional<ASTNode> parseAddExp() {
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
            ASTNode add = new ASTNode(GrammarType.ADD_EXP);
            add.addChild(mulExp.get());
            addExp.replaceLastChild(add);

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
     * RelExp -> AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
     * <p>
     * RelExp -> AddExp { ('<' | '>' | '<=' | '>=') AddExp }
     *
     * @return Optional representing the parsed RelExp
     */
    private Optional<ASTNode> parseRelExp() {
        int initIndex = nowIndex;
        var relExp = begin(GrammarType.REL_EXP);

        var addExp = parseAddExp();
        if (addExp.isPresent()) {
            relExp.addChild(addExp.get());
        } else {
            return failed(initIndex);
        }

        Optional<ASTLeaf> operator;
        while ((operator = parseTerminal(GrammarType.LESS_THAN, GrammarType.LESS_THAN_EQUAL, GrammarType.GREATER_THAN, GrammarType.GREATER_THAN_EQUAL)).isPresent()) {
            ASTNode rel = new ASTNode(GrammarType.REL_EXP);
            rel.addChild(addExp.get());

            relExp.replaceLastChild(rel);
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
     * EqExp -> RelExp | EqExp ('==' | '!=') RelExp
     * <p>
     * EqExp -> RelExp { ('==' | '!=') RelExp }
     *
     * @return Optional representing the parsed EqExp
     */
    private Optional<ASTNode> parseEqExp() {
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
            ASTNode eq = new ASTNode(GrammarType.EQ_EXP);
            eq.addChild(relExp.get());
            eqExp.replaceLastChild(eq);

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
     * LAndExp -> EqExp | LAndExp '&&' EqExp
     * <p>
     * LAndExp -> EqExp { '&&' EqExp}
     *
     * @return Optional<ASTNode> representing the parsed LAndExp
     */
    private Optional<ASTNode> parseLAndExp() {
        int initIndex = nowIndex;
        var lAndExp = begin(GrammarType.LAND_EXP);

        Optional<ASTNode> EqExp;
        EqExp = parseEqExp();
        if (EqExp.isPresent()) lAndExp.addChild(EqExp.get());
        else return failed(initIndex);

        Optional<ASTLeaf> logicalAnd;
        while ((logicalAnd = parseTerminal(GrammarType.LOGICAL_AND)).isPresent()) {
            ASTNode lAnd = new ASTNode(GrammarType.LAND_EXP);
            lAnd.addChild(EqExp.get());
            lAndExp.replaceLastChild(lAnd);

            lAndExp.addChild(logicalAnd.get());
            EqExp = parseEqExp();
            if (EqExp.isEmpty()) return failed(initIndex);
            lAndExp.addChild(EqExp.get());
        }
        return done(lAndExp);
    }

    /**
     * LOrExp -> LAndExp | LOrExp '||' LAndExp
     * <p>
     * LOrExp -> LAndExp { '||' LAndExp }
     *
     * @return Optional<ASTNode> representing the parsed LOrExp
     */
    private Optional<ASTNode> parseLOrExp() {
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
            ASTNode lOr = new ASTNode(GrammarType.LOR_EXP);
            lOr.addChild(lAndExp.get());
            LOrExp.replaceLastChild(lOr);

            LOrExp.addChild(logicalOr.get());
            lAndExp = parseLAndExp();
            if (lAndExp.isEmpty()) return failed(initIndex);
            LOrExp.addChild(lAndExp.get());
        }
        return done(LOrExp);
    }

    /**
     * ConstExp -> AddExp
     *
     * @return Optional<ASTNode> representing the parsed ConstExp
     */
    private Optional<ASTNode> parseConstExp() {
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
    private Optional<ASTLeaf> parseTerminal(GrammarType... type) {
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
