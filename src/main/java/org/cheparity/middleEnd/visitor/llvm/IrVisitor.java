package middleEnd.visitor.llvm;

import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import middleEnd.visitor.ASTNodeVisitor;
import middleEnd.visitor.llvm.ir.Function;
import middleEnd.visitor.llvm.ir.IrBuilder;
import middleEnd.visitor.llvm.ir.IrType;
import middleEnd.visitor.llvm.ir.Module;

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

        int value;
        if (decl.getGrammarType() == GrammarType.CONST_DECL) {
//            ConstDecl -> 'const' BType ConstDef { ',' ConstDef } ';'
            for (var constDef : decl.getChildren()) {
                if (constDef.getGrammarType() != GrammarType.CONST_DEF) continue;
                String name = "@" + constDef.getChild(0).getRawValue();
                value = IrUtil.CalculateConst(constDef.getChild(2));
                builder.buildGlobalConstantValue(module, IrType.Int32TyID, name, value);
            }
        } else {
//            VarDecl -> BType VarDef { ',' VarDef } ';'
            for (var varDef : decl.getChildren()) {
                if (varDef.getGrammarType() != GrammarType.VAR_DEF) continue;
                String name = "@" + varDef.getChild(0).getRawValue();
                if (varDef.getChildren().size() == 1) {
                    //VarDef -> Ident
                    builder.buildGlobalVariable(module, IrType.Int32TyID, name);
                } else {
                    //VarDef -> Ident '=' InitVal
                    value = IrUtil.CalculateConst(varDef.getChild(2));
                    builder.buildGlobalConstantValue(module, IrType.Int32TyID, name, value);
                }
            }
        }
    }


    private void visitFuncDef(ASTNode func, Module module) {
        assert func.getGrammarType().equals(GrammarType.FUNC_DEF) || func.getGrammarType().equals(GrammarType.MAIN_FUNC_DEF);
        IrType funcType = func.getChild(0).getGrammarType().equals(GrammarType.VOID) ? IrType.VoidTyID : IrType.Int32TyID;
        String funcName = func.getChild(1).getRawValue();
        Function function = builder.buildFunction(funcType, "@" + funcName, module);
        for (var child : func.getChildren()) {
            if (child.getGrammarType().equals(GrammarType.FUNC_RPARAMS)) {
                for (ASTNode param : child.getChildren()) {
                    //todo 增加args
                }
            } else if (child.getGrammarType().equals(GrammarType.BLOCK)) {
                var basicBlock = builder.buildBasicBlock("%entry", function);
                for (ASTNode item : child.getChildren()) {
                    if (item.getGrammarType() != GrammarType.BLOCK_ITEM) continue;
                    //blockItem -> Decl | Stmt
                    var decOrStmt = item.getChild(0);
                    if (decOrStmt.getGrammarType() == GrammarType.DECL) {
                        //handle local variables
                        if (decOrStmt.getChild(0).getGrammarType() != GrammarType.VAR_DECL) continue;
                        //VarDecl -> BType VarDef { ',' VarDef } ';'
                        for (var varDef : decOrStmt.getChildren()) {
                            if (varDef.getGrammarType() != GrammarType.VAR_DEF) continue;
                            String name = "%" + varDef.getChild(0).getRawValue();
                            if (varDef.getChildren().size() == 1) {
                                //VarDef -> Ident
                                builder.buildLocalVariable(function, IrType.Int32TyID, name);
                            } else {
                                //VarDef -> Ident '=' InitVal
                                int value = IrUtil.CalculateConst(varDef.getChild(2));
                                builder.buildGlobalConstantValue(module, IrType.Int32TyID, name, value);
                            }
                        }
                        //注意，在local variables中，即使是const，也是开辟一段空间
                        //todo 如果后面有引用const的值，则直接替换为const的值。符号表已经存下来了，所以我们应该不用考虑const？

                    } else if (decOrStmt.getChild(0).getGrammarType() == GrammarType.RETURN) {
                        //stmt especially return stmt
                        var res = IrUtil.CalculateConst(decOrStmt.getChild(1));
                        builder.buildRetInstOfConst(basicBlock, res);
                    } else {
                        //todo 处理一般的stmt
                    }
                }
            }
        }
    }


    private void visitConstExp(ASTNode exp) {
        int result = IrUtil.CalculateConst(exp);

    }

}

