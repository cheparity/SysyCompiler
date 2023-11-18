package middleEnd.llvm.visitor;

import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import middleEnd.ASTNodeVisitor;
import middleEnd.llvm.SSARegisterAllocator;
import middleEnd.llvm.ir.Function;
import middleEnd.llvm.ir.IrBuilder;
import middleEnd.llvm.ir.IrType;
import middleEnd.llvm.ir.Module;

public final class FuncVisitor implements ASTNodeVisitor {
    private final Module module;
    private final IrBuilder builder = new IrBuilder(new SSARegisterAllocator());

    public FuncVisitor(Module module) {
        this.module = module;
    }

    @Override
    public void visit(ASTNode func) {
        //所有的node都是FuncDef. FuncDef -> FuncType Ident '(' [FuncFParams] ')' Block
        assert func.getGrammarType() == GrammarType.FUNC_DEF
                || func.getGrammarType() == GrammarType.MAIN_FUNC_DEF;
        IrType.IrTypeID funcType = func.getChild(0).getGrammarType().equals(GrammarType.VOID) ? IrType.IrTypeID.VoidTyID :
                IrType.IrTypeID.Int32TyID;
        String funcName = func.getChild(1).getRawValue();
        Function function = builder.buildFunction(funcType, funcName, module);
        //todo 解析参数
        func.accept(new BlockVisitor(function, builder));
    }
}
