package middleEnd.llvm.visitor;

import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import frontEnd.symbols.Symbol;
import frontEnd.symbols.SymbolTable;
import middleEnd.ASTNodeVisitor;
import middleEnd.llvm.IrUtil;
import middleEnd.llvm.ir.IrBuilder;
import middleEnd.llvm.ir.IrType;
import middleEnd.llvm.ir.Module;

public final class GlobalVarVisitor implements ASTNodeVisitor {
    private final Module module;
    private final SymbolTable globalSymbolTable = SymbolTable.getGlobal();
    private final IrBuilder builder = new IrBuilder(); //全局，故自行建立一个builder

    public GlobalVarVisitor(Module module) {
        this.module = module;
    }

    @Override
    public void visit(ASTNode node) {
        assert node.getGrammarType() == GrammarType.DECL;
        // Decl -> ConstDecl | VarDecl
        if (node.getChild(0).getGrammarType() == GrammarType.CONST_DECL) {
            visitConstDecl(node.getChild(0));
        } else {
            visitVarDecl(node.getChild(0));
        }
    }

    private void visitVarDecl(ASTNode varDecl) {
        for (var varDef : varDecl.getChildren()) {
            if (varDef.getGrammarType() != GrammarType.VAR_DEF) continue;
            String name = "@" + varDef.getChild(0).getRawValue();
            if (varDef.getChildren().size() == 1) {
                //VarDef -> Ident
                builder.buildGlobalVariable(module, IrType.Int32TyID, name);
                continue;
            }
            //VarDef -> Ident '=' InitVal（一定有确切数字值）
            var number = IrUtil.CalculateConst(varDef.getChild(2), SymbolTable.getGlobal());
            //将value加符号表
            var variable = builder.buildGlobalVariable(module, IrType.Int32TyID, name, number);
            assert globalSymbolTable.getSymbol(name).isPresent();
            Symbol symbol = globalSymbolTable.getSymbol(name).get();
            symbol.setIrVariable(variable);
        }
    }

    private void visitConstDecl(ASTNode constDecl) {
        //ConstDecl -> 'const' BType ConstDef { ',' ConstDef } ';'
        //默认了BType是int
        for (var constDef : constDecl.getChildren()) {
            if (constDef.getGrammarType() != GrammarType.CONST_DEF) continue;
            //直接以变量名命名
            String name = "@" + constDef.getChild(0).getRawValue();
            //常量计算一定有确定的值，在错误处理阶段检查过
            var number = IrUtil.CalculateConst(constDef.getChild(2), globalSymbolTable);
            var variable = builder.buildGlobalConstantValue(module, IrType.Int32TyID, name, number);
            assert globalSymbolTable.getSymbol(name).isPresent();
            Symbol symbol = globalSymbolTable.getSymbol(name).get();
            symbol.setIrVariable(variable);
        }
    }

}
