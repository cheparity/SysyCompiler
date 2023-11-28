package middleEnd.llvm.visitor;

import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import middleEnd.ASTNodeVisitor;
import middleEnd.llvm.SSARegisterAllocator;
import middleEnd.llvm.ir.Function;
import middleEnd.llvm.ir.IrBuilder;
import middleEnd.llvm.ir.IrType;
import middleEnd.llvm.ir.Module;
import middleEnd.symbols.FuncSymbol;
import middleEnd.symbols.SymbolTable;

import java.util.Optional;

public final class FuncVisitor implements ASTNodeVisitor {
    private final Module module;
    private Function function;
    private SymbolTable table;

    public FuncVisitor(Module module) {
        this.module = module;
    }

    @Override
    public void visit(ASTNode func) {
        IrBuilder builder = new IrBuilder(new SSARegisterAllocator());
        //所有的node都是FuncDef. FuncDef -> FuncType Ident '(' [FuncFParams] ')' Block
        //FuncType -> 'void' | 'int'
        assert func.getGrammarType() == GrammarType.FUNC_DEF
                || func.getGrammarType() == GrammarType.MAIN_FUNC_DEF;
        this.table = func.getChild(-1).getSymbolTable();
        IrType.IrTypeID funcType;
        if (func.getGrammarType() == GrammarType.MAIN_FUNC_DEF) {
            funcType = IrType.IrTypeID.Int32TyID;
        } else {
            funcType = func.getChild(0).getChild(0).getGrammarType() == GrammarType.VOID ?
                    IrType.IrTypeID.VoidTyID :
                    IrType.IrTypeID.Int32TyID;
        }
        String funcName = func.getChild(1).getRawValue();
        function = builder.buildFunction(funcType, funcName, module);
        //在全局符号表中注册这个函数
        assert SymbolTable.getGlobal().getFuncSymbol(funcName).isPresent();
        FuncSymbol funcSymbol = SymbolTable.getGlobal().getFuncSymbol(funcName).get();
        funcSymbol.setFunction(function);
        //解析参数
        Optional<ASTNode> paramOpt = func.deepDownFind(GrammarType.FUNC_FPARAMS, 1);
        if (paramOpt.isPresent()) {
            // FuncFParams -> FuncFParam { ',' FuncFParam }
            ASTNode params = paramOpt.get();
            params.getChildren().stream()
                    .filter(node -> node.getGrammarType() == GrammarType.FUNC_FPARAM)
                    .forEach(node -> visitFuncParams(node, builder));
        }
        //这里应该新建一个块，然后把新建的块传递过去
        func.accept(new BlockVisitor(function, builder));
    }

    private void visitFuncParams(ASTNode funcFParam, IrBuilder builder) {
        //FuncFParam -> BType Ident ['[' ']' { '[' ConstExp ']' }]
        //function是没有symbolTable的，symbolTable在其block里
        assert table != null;
        //Symbol是abstract的，实际是varSymbol。varSymbol里记载了维度信息（但是我们这里暂不考虑数组，故一概认为是i32）
        String name = funcFParam.getChild(1).getRawValue();
        assert table.getSymbol(name).isPresent();
        var symbol = table.getSymbol(name).get();

        builder.buildArg(function, IrType.create(IrType.IrTypeID.Int32TyID)); //todo 后续考虑数组的情况
        //不光要build，还要把形参store进对应的block里。。这一块儿由builder.buildEntryBlock来做
    }
}
