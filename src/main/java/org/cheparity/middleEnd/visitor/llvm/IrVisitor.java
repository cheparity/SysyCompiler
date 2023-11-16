package middleEnd.visitor.llvm;

import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import frontEnd.symbols.Symbol;
import frontEnd.symbols.SymbolTable;
import middleEnd.visitor.ASTNodeVisitor;
import middleEnd.visitor.llvm.ir.Module;
import middleEnd.visitor.llvm.ir.*;

public class IrVisitor implements ASTNodeVisitor {
    private static IrVisitor instance;
    private final IrBuilder builder = new IrBuilder();
    private final IrContext context = new IrContext();

    private IrVisitor() {
    }

    public static IrVisitor getInstance() {
        if (instance == null) {
            instance = new IrVisitor();
            return instance;
        }
        return instance;
    }

    public IrContext getContext() {
        return context;
    }

    @Override
    public void visit(ASTNode node) {
        visitCompUnit(node);
    }

    //CompUnit -> {Decl} {FuncDef} MainFuncDef
    private void visitCompUnit(ASTNode root) {
        Module module = builder.buildModule(context);
        context.setIrModule(module);
        for (ASTNode child : root.getChildren()) {
            if (child.getGrammarType() == GrammarType.FUNC_DEF | child.getGrammarType() == GrammarType.MAIN_FUNC_DEF) {
                visitFuncDef(child, module);
            } else if (child.getGrammarType() == GrammarType.DECL) {
                visitGlobalVarDecl(child.getChild(0), module);
            }
        }
    }

    //    CompUnit     -> {Decl} MainFuncDef
    //    Decl         -> ConstDecl | VarDecl
    //    ConstDecl    -> 'const' BType ConstDef { ',' ConstDef } ';'
    //    BType        -> 'int'
    //    ConstDef     -> Ident  '=' ConstInitVal
    //    ConstInitVal -> ConstExp
    //    ConstExp     -> AddExp
    //    VarDecl      -> BType VarDef { ',' VarDef } ';'
    //    VarDef       -> Ident | Ident '=' InitVal
    //    InitVal      -> Exp
    private void visitGlobalVarDecl(ASTNode decl, Module module) {
        assert decl.getGrammarType().equals(GrammarType.DECL);

        int number;
        if (decl.getGrammarType() == GrammarType.CONST_DECL) {
//            ConstDecl -> 'const' BType ConstDef { ',' ConstDef } ';'
            for (var constDef : decl.getChildren()) {
                if (constDef.getGrammarType() != GrammarType.CONST_DEF) continue;
                String name = "@" + constDef.getChild(0).getRawValue();
                //常量计算一定有确定的值
                number = IrUtil.CalculateConst(constDef.getChild(2), SymbolTable.getGlobal());
                builder.buildGlobalConstantValue(module, IrType.Int32TyID, name, number);
            }
            return;
        }
//      VarDecl -> BType VarDef { ',' VarDef } ';'
        for (var varDef : decl.getChildren()) {
            if (varDef.getGrammarType() != GrammarType.VAR_DEF) continue;
            String name = "@" + varDef.getChild(0).getRawValue();
            if (varDef.getChildren().size() == 1) {
                //VarDef -> Ident
                builder.buildGlobalVariable(module, IrType.Int32TyID, name);
            } else {
                //VarDef -> Ident '=' InitVal
                number = IrUtil.CalculateConst(varDef.getChild(2), SymbolTable.getGlobal());
                builder.buildGlobalVariable(module, IrType.Int32TyID, name, number);
            }
        }

    }


    private void visitFuncDef(ASTNode func, Module module) {
        assert func.getGrammarType() == GrammarType.FUNC_DEF || func.getGrammarType() == GrammarType.MAIN_FUNC_DEF;
        //寄存器分配是函数级别的，所以每到一个新函数，就new一个寄存器分配器
        RegisterAllocator allocator = new SSARegisterAllocator();
        IrType funcType = func.getChild(0).getGrammarType().equals(GrammarType.VOID) ? IrType.VoidTyID : IrType.Int32TyID;
        String funcName = func.getChild(1).getRawValue();
        Function function = builder.buildFunction(funcType, "@" + funcName, module);
        for (var child : func.getChildren()) {
            if (child.getGrammarType() == GrammarType.FUNC_RPARAMS) {
                for (ASTNode param : child.getChildren()) {
                    //todo 增加args
                }
            } else if (child.getGrammarType() != GrammarType.BLOCK) continue;
            //to just parse the block
            SymbolTable symbolTable = child.getSymbolTable(); //child现在是block，每个block都有一个符号表
            assert symbolTable != null;
            var basicBlock = builder.buildBasicBlock("%entry", function);
            basicBlock.setSymbolTable(symbolTable);
            for (ASTNode item : child.getChildren()) {
                //只解析blockItem
                if (item.getGrammarType() != GrammarType.BLOCK_ITEM) continue;
                //blockItem -> Decl | Stmt
                var decOrStmt = item.getChild(0);
                if (decOrStmt.getGrammarType() == GrammarType.DECL) {
                    //handle local variables
                    if (decOrStmt.getChild(0).getGrammarType() == GrammarType.CONST_DECL) continue;
                    //VarDecl -> BType VarDef { ',' VarDef } ';'
                    for (var varDef : decOrStmt.getChildren()) {
                        if (varDef.getGrammarType() != GrammarType.VAR_DEF) continue; //仅仅取出varDef
                        visitLocalVarDef(varDef, basicBlock, allocator);
                    }
                } else if (decOrStmt.getChild(0).getGrammarType() == GrammarType.RETURN) {
                    //stmt中的return stmt特殊处理
                    var res = IrUtil.CalculateConst(decOrStmt.getChild(1), symbolTable);
                    builder.buildRetInstOfConst(basicBlock, res);
                } else {
                    //todo 处理一般的stmt
                }
            }
        }
    }

    /**
     * 包括：
     * <p>
     * 1. 分配寄存器，找到对应的符号表，将寄存器的值存入符号中；建立索引，可以根据符号找到寄存器。
     * <p>
     * 2. 交给builder，生成alloca指令，将寄存器的值存入内存中。
     *
     * @param varDef     varDef节点
     * @param basicBlock basicBlock容器，用于存放指令。SymbolTable包含于其中。
     * @param register   寄存器的值，由visitFuncDef分配
     */
    private void visitLocalVarDef(ASTNode varDef, BasicBlock basicBlock, RegisterAllocator allocator) {
        //VarDef -> Ident { '[' ConstExp ']' } ['=' InitVal]
        //查表获得符号 -> 添加到符号表
        var varRawName = varDef.getChild(0).getRawValue();
        var symbolTable = basicBlock.getSymbolTable();
        assert symbolTable.lookup(varRawName).isPresent();
        Symbol symbol = symbolTable.lookup(varRawName).get();
        String name = allocator.allocate(symbol);

        if (varDef.getChildren().size() == 1) {
            //没有初始值
            //VarDef -> Ident
            builder.buildLocalVariable(basicBlock, IrType.Int32TyID, name);
            return;
        }
        //有初始值的情况，此时[value]很可能是一个寄存器的地址（CalculateConst无法得出具体的数字）。这个怎么处理呢？
        //VarDef -> Ident '=' InitVal
        var value = IrUtil.CalculateConst(varDef.getChild(2), symbolTable);

        //注意，在local variables中，即使是const，也是开辟一段空间
        //todo 如果后面有引用const的值，则直接替换为const的值。符号表已经存下来了，所以我们应该不用考虑const？
    }

}

