package middleEnd.llvm.visitor;

import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import middleEnd.ASTNodeVisitor;
import middleEnd.llvm.ir.GlobalValue;
import middleEnd.llvm.ir.IrBuilder;
import middleEnd.llvm.ir.IrType;
import middleEnd.llvm.ir.Module;
import middleEnd.symbols.ConstSymbol;
import middleEnd.symbols.Symbol;
import middleEnd.symbols.SymbolTable;
import utils.Message;

import java.util.ArrayList;

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

    @Override
    public IrBuilder getBuilder() {
        return this.builder;
    }

    @Override
    public void emit(Message message, ASTNodeVisitor sender) {
        //do nothing
    }

    private void visitVarDecl(ASTNode varDecl) {
        for (var varDef : varDecl.getChildren()) {
            if (varDef.getGrammarType() != GrammarType.VAR_DEF) continue;
            String name = varDef.getChild(0).getRawValue();
            Symbol symbol = globalSymbolTable.getSymbolSafely(name, varDecl);

            //首先判断一下是不是数组
            boolean isArr = varDef.deepDownFind(GrammarType.LEFT_BRACKET, 1).isPresent();
            if (!isArr) {
                if (varDef.getChildren().size() == 1) {
                    //VarDef -> Ident
                    var variable = builder.buildGlobalVariable(module, IrType.IrTypeID.Int32TyID, name);
                    symbol.setPointer(variable);
                    continue;
                }
                //VarDef -> Ident '=' InitVal（一定有确切数字值）
                var number = IrUtil.calculateConst4Global(varDef.getChild(2).getChild(0));
                //将value加符号表
                var variable = builder.buildGlobalVariable(module, IrType.IrTypeID.Int32TyID, name, number);
                //是的，还要分配指针
                symbol.setPointer(variable);
                continue;
            }
            //如果是数组
            //VarDef -> Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal
            int dim = symbol.getDim();
            int arrSize = 0;
            for (int i = 0; i < dim; i++) {
                //计算数组大小
                //constExp 出现于 2,5,8...，即 2+3i
                var constExp = varDef.getChild(2 + 3 * i);
                var num = IrUtil.calculateConst4Global(constExp);
                symbol.setDimSize(i + 1, num);
                arrSize = (arrSize == 0) ? num : num * arrSize;
            }

            boolean hasInitVal = varDef.getChild(-1).getGrammarType() == GrammarType.INIT_VAL;
            if (!hasInitVal) {
                GlobalValue globalArray = builder.buildGlobalArray(
                        module, IrType.IrTypeID.Int32TyID, false, arrSize, name);
                symbol.setPointer(globalArray);
                continue;
            }
            //InitVal -> Exp | '{' [ InitVal { ',' InitVal } ] '}'
            var inits = new ArrayList<Integer>();
            IrUtil.unwrapArrayInitVal4Global(varDef.getChild(-1), inits);
            var arrVar = builder.buildGlobalArray(
                    module, IrType.IrTypeID.Int32TyID, false, arrSize, name, inits.toArray(new Integer[0]));
            symbol.setPointer(arrVar);
        }
    }

    private void visitConstDecl(ASTNode constDecl) {
        //ConstDecl -> 'const' BType ConstDef { ',' ConstDef } ';'
        //ConstDef -> Ident { '[' ConstExp ']' } '=' ConstInitVal
        //默认了BType是int
        for (var constDef : constDecl.getChildren()) {
            if (constDef.getGrammarType() != GrammarType.CONST_DEF) continue;
            //直接以变量名命名
            String name = constDef.getChild(0).getRawValue(); //ident
            Symbol symbol = globalSymbolTable.getSymbolSafely(name, constDecl);

            //常量计算一定有确定的值，在错误处理阶段检查过
            //ConstInitVal → ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
            if (constDef.getChildren().size() == 3) {
                //说明是数值 ConstDef -> Ident '=' ConstInitVal
                var number = IrUtil.calculateConst4Global(constDef.getChild(-1));
                var variable = builder.buildGlobalConstantValue(module, IrType.IrTypeID.Int32TyID, name, number);
                symbol.setPointer(variable);
                continue;
            }

            //否则是数组 ConstDef -> Ident { '[' ConstExp ']' } '=' ConstInitVal
            //eg.
            //int a[3+3]={1,2,3,4,5,6};
            //int b[3][3]={{3,6+2,5},{1,2,0},{0,0,0}};
            assert symbol instanceof ConstSymbol;
            var constSymbol = (ConstSymbol) symbol;
            int dim = constSymbol.getDim();
            int arrSize = 0;
            for (int i = 0; i < dim; i++) {
                //计算数组大小
                //constExp 出现于 2,5,8...，即 2+3i
                var constExp = constDef.getChild(2 + 3 * i);
                var num = IrUtil.calculateConst4Global(constExp);
                symbol.setDimSize(i + 1, num);
                arrSize = (arrSize == 0) ? num : num * arrSize;
            }
            //一层一层，解析constInitVal？ => 如果是一维数组，就可以单个解析
            // ConstInitVal → ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
            var inits = new ArrayList<Integer>();
            IrUtil.unwrapArrayInitVal4Global(constDef.getChild(-1), inits);
            var arrVar = builder.buildGlobalArray(
                    module, IrType.IrTypeID.Int32TyID, true, arrSize, name, inits.toArray(new Integer[0]));
            symbol.setPointer(arrVar);
        }
    }

}
